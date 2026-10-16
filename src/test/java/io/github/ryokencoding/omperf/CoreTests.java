package io.github.ryokencoding.omperf;

import io.github.ryokencoding.omperf.config.DiagnosticsConfig;
import io.github.ryokencoding.omperf.telemetry.ReportFormatter;
import io.github.ryokencoding.omperf.telemetry.TickSession;
import io.github.ryokencoding.omperf.telemetry.TickWindow;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

/** No external framework: runnable under javac/java or Gradle check. */
public final class CoreTests {
    private static int checks;

    public static void main(String[] args) throws Exception {
        percentilesAndBudget();
        boundariesAndReset();
        randomizedOracle();
        sessionLifecycleAndClock();
        configAdversarial();
        localeAndReporting();
        System.out.println("PASS: " + checks + " checks (core only; no Fabric/Minecraft runtime).");
    }

    private static void percentilesAndBudget() {
        TickWindow window = new TickWindow(100);
        check(window.snapshot() == TickWindow.Snapshot.EMPTY, "empty has no fabricated samples");
        for (int i = 100; i >= 1; i--) window.record(i * 1_000_000L);
        var s = window.snapshot();
        check(s.samples() == 100 && s.minNanos() == 1_000_000L, "count/min");
        check(s.p50Nanos() == 50_000_000L && s.p95Nanos() == 95_000_000L
                && s.p99Nanos() == 99_000_000L, "nearest rank");
        check(s.maxNanos() == 100_000_000L && s.meanNanos() == 50_500_000, "max/mean");
        check(s.slowTicks() == 50 && s.slowTotalNanos() == 3_775_000_000.0, "strict >50ms budget");
        window.record(0);
        check(window.snapshot().maxNanos() == 99_000_000L, "evict oldest, not minimum");
        check(s.maxNanos() == 100_000_000L, "immutable snapshot survives mutation");
    }

    private static void boundariesAndReset() {
        rejects(() -> new TickWindow(0));
        rejects(() -> new TickWindow(65537));
        TickWindow single = new TickWindow(1);
        single.record(0);
        check(single.snapshot().p99Nanos() == 0 && single.size() == 1, "zero-duration sample");
        single.record(Long.MAX_VALUE);
        check(single.snapshot().p50Nanos() == Long.MAX_VALUE, "largest duration");
        rejects(() -> single.record(-1));
        check(single.snapshot().maxNanos() == Long.MAX_VALUE, "invalid sample does not mutate");
        TickWindow huge = new TickWindow(65536);
        for (int i = 0; i < 131072; i++) huge.record(Long.MAX_VALUE);
        check(huge.size() == huge.capacity(), "bounded after repeated wrap");
        check(Double.isFinite(huge.snapshot().meanNanos())
                && huge.snapshot().meanNanos() > 0, "sum cannot overflow negative");
        huge.clear();
        check(huge.snapshot().samples() == 0, "clear resets history");
        huge.record(7);
        check(huge.snapshot().maxNanos() == 7, "clear does not expose stale values");
    }

    private static void randomizedOracle() {
        Random random = new Random(263015L);
        for (int capacity : new int[] {1, 2, 17, 128, 4096}) {
            TickWindow window = new TickWindow(capacity);
            ArrayDeque<Long> reference = new ArrayDeque<>();
            for (int i = 0; i < 25000; i++) {
                if (random.nextInt(1000) == 0) {
                    window.clear();
                    reference.clear();
                } else {
                    long nanos = random.nextInt(250_000_000);
                    window.record(nanos);
                    if (reference.size() == capacity) reference.removeFirst();
                    reference.addLast(nanos);
                }
                check(window.size() == reference.size(), "bounded oracle size");
                if (i % 37 == 0 && !reference.isEmpty()) {
                    long[] sorted = reference.stream().mapToLong(Long::longValue).sorted().toArray();
                    var s = window.snapshot();
                    check(s.minNanos() == sorted[0] && s.maxNanos() == sorted[sorted.length - 1], "oracle extrema");
                    check(s.p50Nanos() == sorted[(int) Math.ceil(sorted.length * .50) - 1], "oracle p50");
                    check(s.p95Nanos() == sorted[(int) Math.ceil(sorted.length * .95) - 1], "oracle p95");
                    check(s.p99Nanos() == sorted[(int) Math.ceil(sorted.length * .99) - 1], "oracle p99");
                    check(s.slowTicks() == Arrays.stream(sorted).filter(x -> x > 50_000_000L).count(), "oracle slow ticks");
                    check(s.meanNanos() == Arrays.stream(sorted).average().orElseThrow(), "oracle mean");
                }
            }
        }
    }

    private static void sessionLifecycleAndClock() {
        TickSession session = new TickSession(128);
        session.endTick(20);
        check(session.report(0).samples() == 0, "orphan END ignored");
        check(session.report(1) == null, "report throttled");
        session.startTick(40);
        session.endTick(50);
        check(session.report(TickSession.REPORT_COOLDOWN_NANOS).p50Nanos() == 10, "cooldown boundary");
        session.clear();
        check(session.report(0).samples() == 0, "new session report not throttled");
        session.startTick(100);
        session.startTick(101);
        session.endTick(110);
        check(session.report(TickSession.REPORT_COOLDOWN_NANOS).samples() == 0, "duplicate START invalidates pair");
        session.clear();
        session.startTick(Long.MAX_VALUE - 5);
        session.endTick(Long.MIN_VALUE + 4);
        check(session.report(Long.MAX_VALUE - 5).p50Nanos() == 10, "nanoTime wrap");
        check(session.report(Long.MIN_VALUE + 5) == null, "cooldown across wrap");
        session.clear();
        session.startTick(200);
        session.endTick(199);
        check(session.report(0).samples() == 0, "backward clock ignored");
        session.clear();
        session.startTick(100);
        session.clear();
        session.endTick(200);
        check(session.report(0).samples() == 0, "stop discards unfinished tick");
        // Reconnect / world switch stress: no history or cooldown leaks between sessions.
        for (int i = 0; i < 10000; i++) {
            session.clear();
            session.startTick(i);
            session.endTick(i + 1);
            check(session.report(i).samples() == 1, "repeated session reset");
        }
    }

    private static void configAdversarial() throws Exception {
        Path dir = Files.createTempDirectory("omperf-tests-");
        Path path = dir.resolve("omperf.properties");
        try {
            check(DiagnosticsConfig.load(path).equals(DiagnosticsConfig.DEFAULT), "missing config off");
            check(!Files.exists(path), "does not write config");
            Files.writeString(path, "# UTF-8 comentário\ndiagnostics.enabled=true\ndiagnostics.windowSamples=128\n");
            check(DiagnosticsConfig.load(path).equals(new DiagnosticsConfig(true, 128)), "valid config");
            Files.writeString(path, "diagnostics.windowSamples=65536\n");
            check(!DiagnosticsConfig.load(path).enabled(), "enabled independent of window");
            for (String bad : new String[] {
                    "diagnostics.enabled=TRUE", "diagnostics.enabled=1", "diagnostics.enabled=",
                    "diagnostics.windowSamples=-1", "diagnostics.windowSamples=127",
                    "diagnostics.windowSamples=65537", "diagnostics.windowSamples=2147483648",
                    "diagnostics.windowSamples=1.5", "diagnostics.windowSamples=NaN",
                    "diagnostics.enabld=true", "unknown=false", "diagnostics.enabled=\\uZZZZ",
                    "#".repeat(DiagnosticsConfig.MAX_BYTES + 1)}) {
                Files.writeString(path, bad);
                rejects(() -> DiagnosticsConfig.load(path));
            }
            Files.writeString(path, "#".repeat(DiagnosticsConfig.MAX_BYTES));
            check(DiagnosticsConfig.load(path).equals(DiagnosticsConfig.DEFAULT), "max byte boundary");
            Files.delete(path);
            Files.createDirectory(path);
            try {
                DiagnosticsConfig.load(path);
                throw new AssertionError("directory should fail");
            } catch (java.io.IOException expected) { checks++; }
        } finally {
            Files.deleteIfExists(path);
            Files.delete(dir);
        }
    }

    private static void localeAndReporting() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            TickWindow window = new TickWindow(1);
            window.record(1_500_000);
            String report = ReportFormatter.format(window.snapshot());
            check(report.contains("1.500") && report.contains("Not full-loop"), "stable decimal and scope");
            check(ReportFormatter.format(TickWindow.Snapshot.EMPTY).contains("no completed"), "empty report");
        } finally { Locale.setDefault(original); }
    }

    private static void check(boolean success, String description) {
        checks++;
        if (!success) throw new AssertionError(description);
    }

    private static void rejects(Throwing action) {
        try {
            action.run();
            throw new AssertionError("expected invalid input rejection");
        } catch (IllegalArgumentException expected) { checks++; }
        catch (Exception unexpected) { throw new AssertionError(unexpected); }
    }

    @FunctionalInterface private interface Throwing { void run() throws Exception; }
}

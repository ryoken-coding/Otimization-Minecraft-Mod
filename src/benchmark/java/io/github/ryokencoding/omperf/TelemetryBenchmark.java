package io.github.ryokencoding.omperf;

import io.github.ryokencoding.omperf.telemetry.TickWindow;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Locale;

/** Synthetic collector comparison, NOT a Minecraft benchmark or universal performance claim. */
public final class TelemetryBenchmark {
    private static final int CAPACITY = 4096;
    private static final int INSERTIONS = 200_000;
    private static final int REPORTS = 200;
    private static final long[] INPUT = new long[8192];
    private static volatile long sink;
    private static final com.sun.management.ThreadMXBean ALLOC = allocationBean();

    public static void main(String[] args) {
        long seed = 263015;
        for (int i = 0; i < INPUT.length; i++) {
            seed = seed * 6364136223846793005L + 1;
            INPUT[i] = (seed >>> 1) % 100_000_001;
        }
        verifyEquivalent();
        for (int i = 0; i < 3; i++) {
            exercise(new Ring(), INSERTIONS, REPORTS);
            exercise(new Shift(), INSERTIONS, REPORTS);
        }
        System.err.println("Synthetic telemetry only; capacity=4096; warmup=3 rounds/arm; "
                + "insertions=200000; reports=200; heap=128MiB; allocation counter=" + (ALLOC != null));
        System.out.println("trial,implementation,operation,iterations,ns_per_op,allocated_bytes_per_op");
        for (int trial = 1; trial <= 5; trial++) {
            if (trial % 2 == 1) {
                measure(trial, "shift_reference", new Shift());
                measure(trial, "primitive_ring", new Ring());
            } else {
                measure(trial, "primitive_ring", new Ring());
                measure(trial, "shift_reference", new Shift());
            }
        }
        System.err.println("Checksum=" + sink + "; equivalence PASS. No game/runtime performance measured.");
    }

    private static void verifyEquivalent() {
        Recorder ring = new Ring();
        Recorder shift = new Shift();
        for (int i = 0; i < CAPACITY * 3; i++) {
            ring.record(INPUT[i & 8191]);
            shift.record(INPUT[i & 8191]);
        }
        if (!ring.snapshot().equals(shift.snapshot())) throw new AssertionError("collector semantics differ");
    }

    private static void measure(int trial, String name, Recorder recorder) {
        for (int i = 0; i < CAPACITY; i++) recorder.record(INPUT[i & 8191]);
        long bytes = allocated();
        long start = System.nanoTime();
        for (int i = 0; i < INSERTIONS; i++) recorder.record(INPUT[i & 8191]);
        long elapsed = System.nanoTime() - start;
        long delta = ALLOC == null ? -1 : allocated() - bytes;
        sink ^= recorder.snapshot().p99Nanos();
        row(trial, name, "record", INSERTIONS, elapsed, delta);
        bytes = allocated();
        start = System.nanoTime();
        for (int i = 0; i < REPORTS; i++) sink ^= recorder.snapshot().p95Nanos();
        elapsed = System.nanoTime() - start;
        delta = ALLOC == null ? -1 : allocated() - bytes;
        row(trial, name, "snapshot", REPORTS, elapsed, delta);
    }

    private static void row(int trial, String name, String operation, int count, long nanos, long bytes) {
        System.out.printf(Locale.ROOT, "%d,%s,%s,%d,%.3f,%.3f%n", trial, name,
                operation, count, (double) nanos / count, bytes < 0 ? -1 : (double) bytes / count);
    }

    private static void exercise(Recorder recorder, int records, int reports) {
        for (int i = 0; i < records; i++) recorder.record(INPUT[i & 8191]);
        for (int i = 0; i < reports; i++) sink ^= recorder.snapshot().maxNanos();
    }

    private static com.sun.management.ThreadMXBean allocationBean() {
        if (ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean bean
                && bean.isThreadAllocatedMemorySupported()) {
            bean.setThreadAllocatedMemoryEnabled(true);
            return bean;
        }
        return null;
    }

    private static long allocated() {
        return ALLOC == null ? -1 : ALLOC.getThreadAllocatedBytes(Thread.currentThread().threadId());
    }

    private interface Recorder {
        void record(long nanos);
        TickWindow.Snapshot snapshot();
    }

    private static final class Ring implements Recorder {
        private final TickWindow window = new TickWindow(CAPACITY);
        public void record(long nanos) { window.record(nanos); }
        public TickWindow.Snapshot snapshot() { return window.snapshot(); }
    }

    /** Intentionally simple, bounded chronological reference, not code from Minecraft. */
    private static final class Shift implements Recorder {
        private final long[] samples = new long[CAPACITY];
        private int size;
        public void record(long nanos) {
            if (size == samples.length) {
                System.arraycopy(samples, 1, samples, 0, samples.length - 1);
                samples[samples.length - 1] = nanos;
            } else samples[size++] = nanos;
        }
        public TickWindow.Snapshot snapshot() {
            if (size == 0) return TickWindow.Snapshot.EMPTY;
            long[] sorted = Arrays.copyOf(samples, size);
            Arrays.sort(sorted);
            double total = 0;
            double slowTotal = 0;
            int slow = 0;
            for (long nanos : sorted) {
                total += nanos;
                if (nanos > TickWindow.TICK_BUDGET_NANOS) {
                    slow++;
                    slowTotal += nanos;
                }
            }
            return new TickWindow.Snapshot(size, sorted[0], sorted[(size * 50 + 99) / 100 - 1],
                    sorted[(size * 95 + 99) / 100 - 1], sorted[(size * 99 + 99) / 100 - 1],
                    sorted[size - 1], total / size, slow, slowTotal);
        }
    }
}

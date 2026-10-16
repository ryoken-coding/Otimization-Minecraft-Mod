package io.github.ryokencoding.omperf.telemetry;

/** One server lifetime, confined to its thread. Clock values supplied by the adapter. */
public final class TickSession {
    public static final long REPORT_COOLDOWN_NANOS = 5_000_000_000L;
    private final TickWindow window;
    private boolean ticking;
    private long startedAt;
    private boolean reported;
    private long lastReport;

    public TickSession(int capacity) { window = new TickWindow(capacity); }

    public void startTick(long now) {
        // A duplicate START invalidates the pair rather than recording an incorrect duration.
        if (ticking) {
            ticking = false;
            return;
        }
        startedAt = now;
        ticking = true;
    }

    public void endTick(long now) {
        if (!ticking) return;
        ticking = false;
        long elapsed = now - startedAt; // nanoTime subtraction handles signed wraparound.
        if (elapsed >= 0) window.record(elapsed);
    }

    /** Null = rate limited. No sorting/allocation when limited. Empty window remains explicit. */
    public TickWindow.Snapshot report(long now) {
        if (reported && now - lastReport < REPORT_COOLDOWN_NANOS) return null;
        reported = true;
        lastReport = now;
        return window.snapshot();
    }

    public void clear() {
        ticking = false;
        startedAt = 0;
        reported = false;
        lastReport = 0;
        window.clear();
    }
}

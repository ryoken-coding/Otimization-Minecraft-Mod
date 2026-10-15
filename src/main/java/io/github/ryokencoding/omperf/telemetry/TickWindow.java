package io.github.ryokencoding.omperf.telemetry;

import java.util.Arrays;

/** Server-thread confined, bounded and allocation-free on record(). Never retains world objects. */
public final class TickWindow {
    public static final long TICK_BUDGET_NANOS = 50_000_000L;
    private final long[] samples;
    private int next;
    private int size;

    public TickWindow(int capacity) {
        if (capacity < 1 || capacity > 65536) throw new IllegalArgumentException("capacity out of range");
        samples = new long[capacity];
    }

    public void record(long nanos) {
        if (nanos < 0) throw new IllegalArgumentException("negative tick duration");
        samples[next] = nanos;
        if (++next == samples.length) next = 0;
        if (size < samples.length) size++;
    }

    public int size() { return size; }
    public int capacity() { return samples.length; }

    public void clear() {
        // No world references or secrets in storage. Stale primitives are never read.
        next = 0;
        size = 0;
    }

    /** Nearest-rank percentiles over only the retained window; no fabricated empty measurements. */
    public Snapshot snapshot() {
        if (size == 0) return Snapshot.EMPTY;
        long[] sorted = Arrays.copyOf(samples, size);
        Arrays.sort(sorted);
        int slow = 0;
        double total = 0;
        double slowTotal = 0;
        for (long nanos : sorted) {
            total += nanos; // double avoids overflowing a long sum on extreme input.
            if (nanos > TICK_BUDGET_NANOS) {
                slow++;
                slowTotal += nanos;
            }
        }
        return new Snapshot(size, sorted[0], rank(sorted, 50), rank(sorted, 95),
                rank(sorted, 99), sorted[size - 1], total / size, slow, slowTotal);
    }

    private static long rank(long[] sorted, int percentile) {
        return sorted[(int) (((long) sorted.length * percentile + 99) / 100) - 1];
    }

    public record Snapshot(int samples, long minNanos, long p50Nanos, long p95Nanos,
                           long p99Nanos, long maxNanos, double meanNanos,
                           int slowTicks, double slowTotalNanos) {
        public static final Snapshot EMPTY = new Snapshot(0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}

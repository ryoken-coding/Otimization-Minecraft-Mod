package io.github.ryokencoding.omperf.telemetry;

import java.util.Locale;

public final class ReportFormatter {
    private ReportFormatter() { }

    public static String format(TickWindow.Snapshot snapshot) {
        if (snapshot.samples() == 0) return "omperf: no completed tick samples (server may be paused).";
        return String.format(Locale.ROOT,
                "omperf: START->END window n=%d; ms p50=%.3f p95=%.3f p99=%.3f max=%.3f; "
                + ">50ms=%d (total %.3f ms). Not full-loop MSPT or measured TPS.",
                snapshot.samples(), snapshot.p50Nanos() / 1e6, snapshot.p95Nanos() / 1e6,
                snapshot.p99Nanos() / 1e6, snapshot.maxNanos() / 1e6,
                snapshot.slowTicks(), snapshot.slowTotalNanos() / 1e6);
    }
}

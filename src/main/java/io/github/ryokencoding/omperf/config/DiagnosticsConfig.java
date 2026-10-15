package io.github.ryokencoding.omperf.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;

/** Read once at initialization. Invalid/missing config leaves diagnostics disabled. */
public record DiagnosticsConfig(boolean enabled, int windowSamples) {
    public static final int MAX_BYTES = 8192;
    public static final int MIN_SAMPLES = 128;
    public static final int MAX_SAMPLES = 65536;
    public static final DiagnosticsConfig DEFAULT = new DiagnosticsConfig(false, 4096);
    private static final Set<String> KEYS = Set.of("diagnostics.enabled", "diagnostics.windowSamples");

    public DiagnosticsConfig {
        if (windowSamples < MIN_SAMPLES || windowSamples > MAX_SAMPLES) {
            throw new IllegalArgumentException("windowSamples outside safe limits");
        }
    }

    /** Does not create or overwrite user files. Caller handles IO/config errors fail-closed. */
    public static DiagnosticsConfig load(Path path) throws IOException {
        if (!Files.exists(path)) return DEFAULT;
        byte[] bytes;
        try (InputStream input = Files.newInputStream(path)) {
            bytes = input.readNBytes(MAX_BYTES + 1);
        }
        if (bytes.length > MAX_BYTES) throw new IllegalArgumentException("config too large");
        Properties properties = new Properties();
        properties.load(new StringReader(new String(bytes, StandardCharsets.UTF_8)));
        if (!KEYS.containsAll(properties.stringPropertyNames())) {
            throw new IllegalArgumentException("unknown config key");
        }
        String enabled = properties.getProperty("diagnostics.enabled", "false").trim();
        if (!enabled.equals("true") && !enabled.equals("false")) {
            throw new IllegalArgumentException("enabled must be true or false");
        }
        int window = Integer.parseInt(properties.getProperty("diagnostics.windowSamples", "4096").trim());
        return new DiagnosticsConfig(Boolean.parseBoolean(enabled), window);
    }
}

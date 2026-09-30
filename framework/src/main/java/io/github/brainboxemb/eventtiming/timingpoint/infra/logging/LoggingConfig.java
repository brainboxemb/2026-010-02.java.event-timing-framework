package io.github.brainboxemb.eventtiming.timingpoint.infra.logging;

/** Effective runtime configuration owned by the reusable logging component. */
public final class LoggingConfig {
    private final LoggingLevel level;
    private final LoggingFileConfig file;

    public LoggingConfig(LoggingLevel level, LoggingFileConfig file) {
        if (level == null) {
            throw new IllegalArgumentException("logging level must not be null");
        }
        if (file == null) {
            throw new IllegalArgumentException("logging file config must not be null");
        }
        this.level = level;
        this.file = file;
    }

    public LoggingLevel level() {
        return level;
    }

    public LoggingFileConfig file() {
        return file;
    }
}

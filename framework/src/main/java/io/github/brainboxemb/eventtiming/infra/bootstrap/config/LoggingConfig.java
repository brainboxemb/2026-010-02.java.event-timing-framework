package io.github.brainboxemb.eventtiming.infra.bootstrap.config;

/** Provider-neutral runtime logging configuration consumed by the executable composition. */
public final class LoggingConfig {
    public enum Level {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    private final Level level;
    private final LoggingFileConfig file;
    private final LoggingLiveConfig live;

    public LoggingConfig(Level level, LoggingFileConfig file, LoggingLiveConfig live) {
        if (level == null) {
            throw new IllegalArgumentException("logging level must not be null");
        }
        if (file == null) {
            throw new IllegalArgumentException("logging file config must not be null");
        }
        this.level = level;
        this.file = file;
        this.live = live;
    }

    public Level level() {
        return level;
    }

    public LoggingFileConfig file() {
        return file;
    }

    public LoggingLiveConfig live() {
        return live;
    }
}

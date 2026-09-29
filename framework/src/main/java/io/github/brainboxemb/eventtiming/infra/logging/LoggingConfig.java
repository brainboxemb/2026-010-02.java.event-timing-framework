package io.github.brainboxemb.eventtiming.infra.logging;

/** Effective runtime configuration owned by the reusable logging component. */
public final class LoggingConfig {
    private final LoggingLevel level;
    private final LoggingFileConfig file;
    private final LoggingServerConfig server;

    public LoggingConfig(
            LoggingLevel level,
            LoggingFileConfig file,
            LoggingServerConfig server) {
        if (level == null) {
            throw new IllegalArgumentException("logging level must not be null");
        }
        if (file == null) {
            throw new IllegalArgumentException("logging file config must not be null");
        }
        this.level = level;
        this.file = file;
        this.server = server;
    }

    public LoggingLevel level() {
        return level;
    }

    public LoggingFileConfig file() {
        return file;
    }

    public LoggingServerConfig server() {
        return server;
    }
}

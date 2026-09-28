package io.github.brainboxemb.eventtiming.app.logging;

import io.github.brainboxemb.eventtiming.infra.bootstrap.config.LoggingConfig;

import java.util.logging.Logger;

/** Owns the configured global logging level plus a temporary runtime override. */
final class LoggingControl {
    private final Logger rootLogger;
    private LoggingConfig.Level level;

    LoggingControl(Logger rootLogger, LoggingConfig.Level initialLevel) {
        this.rootLogger = rootLogger;
        setLevel(initialLevel);
    }

    synchronized LoggingConfig.Level level() {
        return level;
    }

    synchronized void setLevel(LoggingConfig.Level newLevel) {
        if (newLevel == null) {
            throw new IllegalArgumentException("logging level must not be null");
        }
        rootLogger.setLevel(toJulLevel(newLevel));
        level = newLevel;
    }

    static java.util.logging.Level toJulLevel(LoggingConfig.Level level) {
        switch (level) {
            case TRACE:
                return java.util.logging.Level.FINEST;
            case DEBUG:
                return java.util.logging.Level.FINE;
            case INFO:
                return java.util.logging.Level.INFO;
            case WARN:
                return java.util.logging.Level.WARNING;
            case ERROR:
                return java.util.logging.Level.SEVERE;
            default:
                throw new IllegalArgumentException("Unsupported logging level: " + level);
        }
    }

    static String semanticLevel(java.util.logging.Level level) {
        int value = level.intValue();
        if (value >= java.util.logging.Level.SEVERE.intValue()) {
            return "ERROR";
        }
        if (value >= java.util.logging.Level.WARNING.intValue()) {
            return "WARN";
        }
        if (value >= java.util.logging.Level.INFO.intValue()) {
            return "INFO";
        }
        if (value >= java.util.logging.Level.FINE.intValue()) {
            return "DEBUG";
        }
        return "TRACE";
    }
}

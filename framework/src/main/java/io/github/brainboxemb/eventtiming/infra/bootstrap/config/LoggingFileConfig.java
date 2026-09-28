package io.github.brainboxemb.eventtiming.infra.bootstrap.config;

/** Retained operational log-file settings. */
public final class LoggingFileConfig {
    private final String path;
    private final int rotateBytes;
    private final int retainedFiles;

    public LoggingFileConfig(String path, int rotateBytes, int retainedFiles) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("logging file path must not be blank");
        }
        if (rotateBytes <= 0) {
            throw new IllegalArgumentException("rotateBytes must be greater than zero");
        }
        if (retainedFiles <= 0) {
            throw new IllegalArgumentException("retainedFiles must be greater than zero");
        }
        this.path = path.trim();
        this.rotateBytes = rotateBytes;
        this.retainedFiles = retainedFiles;
    }

    public String path() {
        return path;
    }

    public int rotateBytes() {
        return rotateBytes;
    }

    public int retainedFiles() {
        return retainedFiles;
    }
}

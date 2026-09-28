package io.github.brainboxemb.eventtiming.infra.bootstrap.config;

/** Optional engineering live-log listener settings. */
public final class LoggingLiveConfig {
    private final String bindAddress;
    private final int port;

    public LoggingLiveConfig(String bindAddress, int port) {
        if (bindAddress == null || bindAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("logging live bindAddress must not be blank");
        }
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("logging live port must be between 0 and 65535");
        }
        this.bindAddress = bindAddress.trim();
        this.port = port;
    }

    public String bindAddress() {
        return bindAddress;
    }

    public int port() {
        return port;
    }
}

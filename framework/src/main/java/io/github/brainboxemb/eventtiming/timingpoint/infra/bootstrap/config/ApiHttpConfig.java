package io.github.brainboxemb.eventtiming.timingpoint.infra.bootstrap.config;

/** Effective configuration for the IF-03 API HTTP listener. */
public final class ApiHttpConfig {
    private final String bindAddress;
    private final int port;

    public ApiHttpConfig(String bindAddress, int port) {
        if (bindAddress == null || bindAddress.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "api.http bindAddress must not be blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException(
                    "api.http port must be between 1 and 65535");
        }
        this.bindAddress = bindAddress;
        this.port = port;
    }

    public String bindAddress() {
        return bindAddress;
    }

    public int port() {
        return port;
    }
}

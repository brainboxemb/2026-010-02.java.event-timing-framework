package io.github.brainboxemb.eventtiming.infra.bootstrap.config;

/** Effective configuration for the IF-03 API WebSocket listener. */
public final class ApiWebSocketConfig {
    private final String bindAddress;
    private final int port;

    public ApiWebSocketConfig(String bindAddress, int port) {
        if (bindAddress == null || bindAddress.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "api.webSocket bindAddress must not be blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException(
                    "api.webSocket port must be between 1 and 65535");
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

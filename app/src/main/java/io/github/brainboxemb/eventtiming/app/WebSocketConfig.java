package io.github.brainboxemb.eventtiming.app;

/** Effective configuration for the A07 IF-03 WebSocket listener. */
final class WebSocketConfig {
    private final String bindAddress;
    private final int port;

    WebSocketConfig(String bindAddress, int port) {
        if (bindAddress == null || bindAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("webSocket bindAddress must not be blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("webSocket port must be between 1 and 65535");
        }
        this.bindAddress = bindAddress;
        this.port = port;
    }

    String bindAddress() {
        return bindAddress;
    }

    int port() {
        return port;
    }
}

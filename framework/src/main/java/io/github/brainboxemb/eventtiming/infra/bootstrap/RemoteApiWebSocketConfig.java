package io.github.brainboxemb.eventtiming.infra.bootstrap;

/** Effective configuration for the IF-03 Remote API WebSocket listener. */
public final class RemoteApiWebSocketConfig {
    private final String bindAddress;
    private final int port;

    public RemoteApiWebSocketConfig(String bindAddress, int port) {
        if (bindAddress == null || bindAddress.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "remoteApi.webSocket bindAddress must not be blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException(
                    "remoteApi.webSocket port must be between 1 and 65535");
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

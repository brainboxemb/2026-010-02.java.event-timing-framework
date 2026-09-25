package io.github.brainboxemb.eventtiming.app;

/** Effective configuration for the A05 remote shell listener. */
final class RemoteShellConfig {
    private final String bindAddress;
    private final int port;

    RemoteShellConfig(String bindAddress, int port) {
        if (bindAddress == null || bindAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("remote shell bindAddress must not be blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("remote shell port must be between 1 and 65535");
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

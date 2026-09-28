package io.github.brainboxemb.eventtiming.infra.bootstrap.config;

/** Effective configuration for the A05 remote shell listener. */
public final class RemoteShellConfig {
    private final String bindAddress;
    private final int port;

    public RemoteShellConfig(String bindAddress, int port) {
        if (bindAddress == null || bindAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("remote shell bindAddress must not be blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("remote shell port must be between 1 and 65535");
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

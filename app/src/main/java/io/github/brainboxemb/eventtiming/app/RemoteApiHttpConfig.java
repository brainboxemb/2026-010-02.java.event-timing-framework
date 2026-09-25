package io.github.brainboxemb.eventtiming.app;

/** Effective configuration for the IF-03 Remote API HTTP listener. */
final class RemoteApiHttpConfig {
    private final String bindAddress;
    private final int port;

    RemoteApiHttpConfig(String bindAddress, int port) {
        if (bindAddress == null || bindAddress.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "remoteApi.http bindAddress must not be blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException(
                    "remoteApi.http port must be between 1 and 65535");
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

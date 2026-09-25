package io.github.brainboxemb.eventtiming.app;

/** Effective configuration for the A06 HTTP/JSON listener. */
final class HttpConfig {
    private final String bindAddress;
    private final int port;

    HttpConfig(String bindAddress, int port) {
        if (bindAddress == null || bindAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("http bindAddress must not be blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("http port must be between 1 and 65535");
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

package io.github.brainboxemb.eventtiming.app;

/** Effective configuration for the IF-03 Remote API presentation interface. */
final class RemoteApiConfig {
    private final RemoteApiHttpConfig http;
    private final RemoteApiWebSocketConfig webSocket;

    RemoteApiConfig(RemoteApiHttpConfig http, RemoteApiWebSocketConfig webSocket) {
        if (http == null && webSocket == null) {
            throw new IllegalArgumentException(
                    "remoteApi must configure at least one transport");
        }
        this.http = http;
        this.webSocket = webSocket;
    }

    RemoteApiHttpConfig http() {
        return http;
    }

    RemoteApiWebSocketConfig webSocket() {
        return webSocket;
    }
}

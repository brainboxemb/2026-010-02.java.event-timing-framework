package io.github.brainboxemb.eventtiming.infra.bootstrap;

/** Effective configuration for the IF-03 Remote API presentation interface. */
public final class RemoteApiConfig {
    private final RemoteApiHttpConfig http;
    private final RemoteApiWebSocketConfig webSocket;

    public RemoteApiConfig(RemoteApiHttpConfig http, RemoteApiWebSocketConfig webSocket) {
        if (http == null && webSocket == null) {
            throw new IllegalArgumentException(
                    "remoteApi must configure at least one transport");
        }
        this.http = http;
        this.webSocket = webSocket;
    }

    public RemoteApiHttpConfig http() {
        return http;
    }

    public RemoteApiWebSocketConfig webSocket() {
        return webSocket;
    }
}

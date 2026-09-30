package io.github.brainboxemb.eventtiming.infra.bootstrap.config;

/** Effective configuration for the IF-03 API presentation interface. */
public final class ApiConfig {
    private final ApiHttpConfig http;
    private final ApiWebSocketConfig webSocket;

    public ApiConfig(ApiHttpConfig http, ApiWebSocketConfig webSocket) {
        if (http == null && webSocket == null) {
            throw new IllegalArgumentException(
                    "api must configure at least one transport");
        }
        this.http = http;
        this.webSocket = webSocket;
    }

    public ApiHttpConfig http() {
        return http;
    }

    public ApiWebSocketConfig webSocket() {
        return webSocket;
    }
}

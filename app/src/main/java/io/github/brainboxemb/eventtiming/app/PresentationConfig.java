package io.github.brainboxemb.eventtiming.app;

/** Effective presentation configuration for currently implemented adapters. */
final class PresentationConfig {
    private final RemoteShellConfig remoteShell;
    private final HttpConfig http;
    private final WebSocketConfig webSocket;

    PresentationConfig(
            RemoteShellConfig remoteShell,
            HttpConfig http,
            WebSocketConfig webSocket) {
        this.remoteShell = remoteShell;
        this.http = http;
        this.webSocket = webSocket;
    }

    RemoteShellConfig remoteShell() {
        return remoteShell;
    }

    HttpConfig http() {
        return http;
    }

    WebSocketConfig webSocket() {
        return webSocket;
    }
}

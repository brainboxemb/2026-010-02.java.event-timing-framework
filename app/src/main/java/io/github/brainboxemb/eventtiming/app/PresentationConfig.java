package io.github.brainboxemb.eventtiming.app;

/** Effective presentation configuration for currently implemented adapters. */
final class PresentationConfig {
    private final RemoteShellConfig remoteShell;
    private final HttpConfig http;

    PresentationConfig(RemoteShellConfig remoteShell, HttpConfig http) {
        this.remoteShell = remoteShell;
        this.http = http;
    }

    RemoteShellConfig remoteShell() {
        return remoteShell;
    }

    HttpConfig http() {
        return http;
    }
}

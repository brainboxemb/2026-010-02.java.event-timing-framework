package io.github.brainboxemb.eventtiming.app;

/** Effective presentation configuration for currently implemented interfaces. */
final class PresentationConfig {
    private final RemoteShellConfig remoteShell;
    private final RemoteApiConfig remoteApi;

    PresentationConfig(RemoteShellConfig remoteShell, RemoteApiConfig remoteApi) {
        this.remoteShell = remoteShell;
        this.remoteApi = remoteApi;
    }

    RemoteShellConfig remoteShell() {
        return remoteShell;
    }

    RemoteApiConfig remoteApi() {
        return remoteApi;
    }
}

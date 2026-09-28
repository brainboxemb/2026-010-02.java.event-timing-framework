package io.github.brainboxemb.eventtiming.infra.bootstrap;

/** Effective presentation configuration consumed by ApplicationBootstrap. */
public final class PresentationConfig {
    private final RemoteShellConfig remoteShell;
    private final RemoteApiConfig remoteApi;

    public PresentationConfig(RemoteShellConfig remoteShell, RemoteApiConfig remoteApi) {
        this.remoteShell = remoteShell;
        this.remoteApi = remoteApi;
    }

    public RemoteShellConfig remoteShell() {
        return remoteShell;
    }

    public RemoteApiConfig remoteApi() {
        return remoteApi;
    }
}

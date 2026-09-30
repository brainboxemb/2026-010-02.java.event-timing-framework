package io.github.brainboxemb.eventtiming.timingpoint.infra.bootstrap.config;

/** Effective presentation configuration consumed by ApplicationBootstrap. */
public final class PresentationConfig {
    private final RemoteShellConfig remoteShell;
    private final ApiConfig api;

    public PresentationConfig(RemoteShellConfig remoteShell, ApiConfig api) {
        this.remoteShell = remoteShell;
        this.api = api;
    }

    public RemoteShellConfig remoteShell() {
        return remoteShell;
    }

    public ApiConfig api() {
        return api;
    }
}

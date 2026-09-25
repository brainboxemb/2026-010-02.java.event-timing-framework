package io.github.brainboxemb.eventtiming.app;

/** Effective presentation configuration for currently implemented adapters. */
final class PresentationConfig {
    private final RemoteShellConfig remoteShell;

    PresentationConfig(RemoteShellConfig remoteShell) {
        this.remoteShell = remoteShell;
    }

    RemoteShellConfig remoteShell() {
        return remoteShell;
    }
}

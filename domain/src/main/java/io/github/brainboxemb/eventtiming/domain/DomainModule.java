package io.github.brainboxemb.eventtiming.domain;

/** Marker used only to prove the initial domain module boundary. */
public final class DomainModule {
    private DomainModule() {
    }

    public static String name() {
        return "domain";
    }
}

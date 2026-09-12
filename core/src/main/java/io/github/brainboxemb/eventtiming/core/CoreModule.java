package io.github.brainboxemb.eventtiming.core;

import io.github.brainboxemb.eventtiming.domain.DomainModule;

/** Marker used only to prove the initial core -> domain dependency direction. */
public final class CoreModule {
    private CoreModule() {
    }

    public static String composition() {
        return DomainModule.name() + " -> core";
    }
}

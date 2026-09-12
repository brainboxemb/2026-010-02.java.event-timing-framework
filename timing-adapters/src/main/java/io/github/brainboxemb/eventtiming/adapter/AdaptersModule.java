package io.github.brainboxemb.eventtiming.adapter;

import io.github.brainboxemb.eventtiming.api.ApiModule;

/** Minimal bootstrap marker proving adapters depend on public API contracts. */
public final class AdaptersModule {
    private AdaptersModule() {
    }

    public static String composition() {
        return ApiModule.moduleName() + " -> timing-adapters";
    }
}

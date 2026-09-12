package io.github.brainboxemb.eventtiming.core;

import io.github.brainboxemb.eventtiming.api.ApiModule;

/** Minimal bootstrap marker proving core depends inward on the public API. */
public final class CoreModule {
    private CoreModule() {
    }

    public static String composition() {
        return ApiModule.moduleName() + " -> timing-core";
    }
}

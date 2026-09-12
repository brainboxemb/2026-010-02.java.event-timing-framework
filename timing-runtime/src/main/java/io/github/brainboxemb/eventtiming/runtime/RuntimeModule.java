package io.github.brainboxemb.eventtiming.runtime;

import io.github.brainboxemb.eventtiming.core.CoreModule;

/** Minimal bootstrap marker proving runtime depends inward on core. */
public final class RuntimeModule {
    private RuntimeModule() {
    }

    public static String composition() {
        return CoreModule.composition() + " -> timing-runtime";
    }
}

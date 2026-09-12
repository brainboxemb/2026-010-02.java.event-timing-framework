package io.github.brainboxemb.eventtiming.api;

/** Minimal bootstrap marker for the public API module. */
public final class ApiModule {
    private ApiModule() {
    }

    public static String moduleName() {
        return "timing-api";
    }
}

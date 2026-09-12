package io.github.brainboxemb.eventtiming.app;

import io.github.brainboxemb.eventtiming.adapter.AdaptersModule;
import io.github.brainboxemb.eventtiming.runtime.RuntimeModule;

/** Minimal composition root used only to prove the initial module/toolchain skeleton. */
public final class TimingApplication {
    private static final String EXPECTED_COMPOSITION =
            "timing-api -> timing-core -> timing-runtime + timing-api -> timing-adapters";

    private TimingApplication() {
    }

    public static String composition() {
        return RuntimeModule.composition() + " + " + AdaptersModule.composition();
    }

    public static void main(String[] args) {
        if (!EXPECTED_COMPOSITION.equals(composition())) {
            throw new IllegalStateException("Unexpected bootstrap composition: " + composition());
        }
        System.out.println("event-timing-framework bootstrap OK");
    }
}

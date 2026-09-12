package io.github.brainboxemb.eventtiming.app;

import io.github.brainboxemb.eventtiming.core.CoreLayer;

/** Minimal composition root used only to prove the framework-library consumer path. */
public final class TimingApplication {
    private TimingApplication() {
    }

    public static String frameworkComponent() {
        return CoreLayer.name();
    }

    public static void main(String[] args) {
        if (!"core".equals(frameworkComponent())) {
            throw new IllegalStateException("Framework library is not composed as expected.");
        }
        System.out.println("event-timing-framework bootstrap OK");
    }
}

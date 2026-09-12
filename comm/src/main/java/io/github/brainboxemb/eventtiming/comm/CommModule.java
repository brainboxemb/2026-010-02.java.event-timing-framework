package io.github.brainboxemb.eventtiming.comm;

import io.github.brainboxemb.eventtiming.core.CoreModule;
import io.github.brainboxemb.eventtiming.platform.PlatformModule;

/** Marker used only to prove the initial communication module boundary. */
public final class CommModule {
    private CommModule() {
    }

    public static String composition() {
        return CoreModule.composition() + " + " + PlatformModule.name() + " -> comm";
    }
}

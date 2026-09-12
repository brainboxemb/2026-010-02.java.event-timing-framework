package io.github.brainboxemb.eventtiming.platform;

/** Marker used only to prove the initial platform module boundary. */
public final class PlatformModule {
    private PlatformModule() {
    }

    public static String name() {
        return "platform";
    }
}

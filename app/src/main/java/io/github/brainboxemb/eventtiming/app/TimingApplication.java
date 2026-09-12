package io.github.brainboxemb.eventtiming.app;

import io.github.brainboxemb.eventtiming.comm.CommModule;
import io.github.brainboxemb.eventtiming.core.CoreModule;
import io.github.brainboxemb.eventtiming.domain.DomainModule;
import io.github.brainboxemb.eventtiming.platform.PlatformModule;

/** Minimal composition root used only to prove the initial module/toolchain skeleton. */
public final class TimingApplication {
    private TimingApplication() {
    }

    public static int componentCount() {
        String[] components = {
                DomainModule.name(),
                CoreModule.composition(),
                PlatformModule.name(),
                CommModule.composition()
        };
        return components.length;
    }

    public static void main(String[] args) {
        if (componentCount() != 4) {
            throw new IllegalStateException("Unexpected bootstrap component count: " + componentCount());
        }
        System.out.println("event-timing-framework bootstrap OK");
    }
}

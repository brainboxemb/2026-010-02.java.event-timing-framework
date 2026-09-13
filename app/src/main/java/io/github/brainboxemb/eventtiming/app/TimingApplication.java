package io.github.brainboxemb.eventtiming.app;

import io.github.brainboxemb.eventtiming.application.BuildIdentity;
import io.github.brainboxemb.eventtiming.core.CoreLayer;

/**
 * Executable composition root for the current SI-01 application baseline.
 *
 * <p>The class intentionally performs only composition/lifecycle work. Timing-domain behaviour
 * belongs in the reusable framework; later transports and integrations are wired here (or through
 * application-owned composition classes) rather than being hard-coded into framework services.
 * See the SI-01 SAD and Java component design in the meta repository.</p>
 */
public final class TimingApplication {
    private TimingApplication() {
    }

    /** Returns a minimal framework marker used by Step-2 dependency/composition evidence. */
    public static String frameworkComponent() {
        return CoreLayer.name();
    }

    /**
     * Loads the identity embedded by Maven, starts the application lifecycle and always closes it.
     * The final stdout line is deliberately stable because CI uses it as a cross-platform smoke
     * contract; richer build provenance is emitted through normal lifecycle logging.
     */
    public static void main(String[] args) {
        if (!"core".equals(frameworkComponent())) {
            throw new IllegalStateException("Framework library is not composed as expected.");
        }

        BuildIdentity buildIdentity = BuildIdentityLoader.load();
        TimingApplicationLifecycle lifecycle = new TimingApplicationLifecycle(buildIdentity);
        try {
            lifecycle.start();
        } finally {
            lifecycle.close();
        }

        System.out.println(smokeOutput(buildIdentity, lifecycle.state()));
    }

    static String smokeOutput(BuildIdentity buildIdentity, TimingApplicationLifecycle.State state) {
        return "event-timing-app lifecycle OK version=" + buildIdentity.version() + " state=" + state;
    }
}

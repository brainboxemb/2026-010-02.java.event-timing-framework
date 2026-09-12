package io.github.brainboxemb.eventtiming.app;

import io.github.brainboxemb.eventtiming.core.CoreLayer;

/** Step-2 executable composition root. */
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

        BuildIdentity buildIdentity = BuildIdentity.load();
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

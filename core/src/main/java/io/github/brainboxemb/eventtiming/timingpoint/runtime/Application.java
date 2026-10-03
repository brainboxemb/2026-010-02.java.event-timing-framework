package io.github.brainboxemb.eventtiming.timingpoint.runtime;

import io.github.brainboxemb.eventtiming.timingpoint.application.PresentationGateway;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNode;
import io.github.brainboxemb.eventtiming.timingpoint.infra.BuildIdentity;

/** Top-level runtime object for one SI-01 application composition. */
public final class Application implements AutoCloseable {
    private final BuildIdentity buildIdentity;
    private final TimingNode timingNode;
    private final PresentationGateway presentationGateway;
    private final Lifecycle lifecycle;

    Application(BuildIdentity buildIdentity, TimingNode timingNode) {
        if (buildIdentity == null) {
            throw new IllegalArgumentException("buildIdentity must not be null");
        }
        if (timingNode == null) {
            throw new IllegalArgumentException("timingNode must not be null");
        }
        this.buildIdentity = buildIdentity;
        this.timingNode = timingNode;
        this.presentationGateway = new PresentationGateway(buildIdentity, timingNode);
        this.lifecycle = new Lifecycle(buildIdentity);
    }

    public void start() {
        timingNode.start();
        try {
            lifecycle.start();
        } catch (RuntimeException ex) {
            timingNode.stop();
            throw ex;
        }
    }

    public PresentationGateway presentationGateway() {
        return presentationGateway;
    }

    TimingNode timingNode() {
        return timingNode;
    }

    BuildIdentity buildIdentity() {
        return buildIdentity;
    }

    Lifecycle.State state() {
        return lifecycle.state();
    }

    public void awaitStopped() throws InterruptedException {
        lifecycle.awaitStopped();
    }

    public String smokeOutput() {
        return smokeOutput(buildIdentity, lifecycle.state());
    }

    @Override
    public void close() {
        timingNode.stop();
        lifecycle.close();
    }

    public static String smokeOutput(BuildIdentity buildIdentity, Lifecycle.State state) {
        return buildIdentity.application()
                + " lifecycle OK version="
                + buildIdentity.version()
                + " state="
                + state;
    }
}

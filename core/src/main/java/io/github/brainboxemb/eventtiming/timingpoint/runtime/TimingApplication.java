package io.github.brainboxemb.eventtiming.timingpoint.runtime;

import io.github.brainboxemb.eventtiming.timingpoint.application.ApplicationStatus;
import io.github.brainboxemb.eventtiming.timingpoint.application.CommandHandler;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNode;
import io.github.brainboxemb.eventtiming.timingpoint.infra.BuildIdentity;

/**
 * Top-level reusable runtime object for one SI-01 application composition.
 *
 * <p>Concrete configuration parsing and executable input handling stay outside this runtime.
 * Cross-cutting composition is owned by the application-core ApplicationBootstrap.</p>
 */
public final class TimingApplication implements AutoCloseable {
    private final BuildIdentity buildIdentity;
    private final TimingNode timingNode;
    private final CommandHandler commandHandler;
    private final TimingApplicationLifecycle lifecycle;

    private TimingApplication(
            BuildIdentity buildIdentity,
            TimingNode timingNode,
            CommandHandler commandHandler,
            TimingApplicationLifecycle lifecycle) {
        this.buildIdentity = buildIdentity;
        this.timingNode = timingNode;
        this.commandHandler = commandHandler;
        this.lifecycle = lifecycle;
    }

    public static Builder builder(BuildIdentity buildIdentity) {
        return new Builder(buildIdentity);
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

    public CommandHandler commandHandler() {
        return commandHandler;
    }

    TimingNode timingNode() {
        return timingNode;
    }

    BuildIdentity buildIdentity() {
        return buildIdentity;
    }

    TimingApplicationLifecycle.State state() {
        return lifecycle.state();
    }

    public void awaitStopped() throws InterruptedException {
        lifecycle.awaitStopped();
    }

    /** Stable executable smoke/status line used after bootstrap shutdown. */
    public String smokeOutput() {
        return smokeOutput(buildIdentity, lifecycle.state());
    }

    @Override
    public void close() {
        timingNode.stop();
        lifecycle.close();
    }

    public static String smokeOutput(
            BuildIdentity buildIdentity,
            TimingApplicationLifecycle.State state) {
        return buildIdentity.application() + " lifecycle OK version=" + buildIdentity.version() + " state=" + state;
    }

    /** Small runtime builder that creates only objects required by the current application-core runtime. */
    public static final class Builder {
        private final BuildIdentity buildIdentity;
        private TimingNode timingNode;

        private Builder(BuildIdentity buildIdentity) {
            if (buildIdentity == null) {
                throw new IllegalArgumentException("buildIdentity must not be null");
            }
            this.buildIdentity = buildIdentity;
        }

        public Builder timingNode(TimingNode timingNode) {
            if (timingNode == null) {
                throw new IllegalArgumentException("timingNode must not be null");
            }
            this.timingNode = timingNode;
            return this;
        }

        public TimingApplication build() {
            if (timingNode == null) {
                throw new IllegalStateException("timingNode must be configured before build");
            }
            TimingApplicationLifecycle lifecycle = new TimingApplicationLifecycle(buildIdentity);
            CommandHandler commandHandler = new CommandHandler(
                    buildIdentity,
                    () -> {
                        TimingNode.Status status = timingNode.status();
                        return new ApplicationStatus(
                                status.timingNodeId(),
                                status.lifecycle());
                    });
            return new TimingApplication(
                    buildIdentity,
                    timingNode,
                    commandHandler,
                    lifecycle);
        }
    }
}

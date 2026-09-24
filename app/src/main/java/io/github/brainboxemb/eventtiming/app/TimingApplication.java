package io.github.brainboxemb.eventtiming.app;

import io.github.brainboxemb.eventtiming.application.BuildIdentity;
import io.github.brainboxemb.eventtiming.application.CommandHandler;

/**
 * Executable composition root for the current SI-01 application baseline.
 *
 * <p>The class performs composition/lifecycle work and exposes the shared application boundary
 * needed by presentation adapters. Timing-domain behaviour belongs in the reusable framework;
 * later transports and integrations are wired here rather than represented by layer-marker
 * objects.</p>
 */
public final class TimingApplication implements AutoCloseable {
    private final CommandHandler commandHandler;
    private final TimingApplicationLifecycle lifecycle;

    private TimingApplication(
            CommandHandler commandHandler,
            TimingApplicationLifecycle lifecycle) {
        this.commandHandler = commandHandler;
        this.lifecycle = lifecycle;
    }

    public static Builder builder(BuildIdentity buildIdentity) {
        return new Builder(buildIdentity);
    }

    public void start() {
        lifecycle.start();
    }

    public CommandHandler commandHandler() {
        return commandHandler;
    }

    TimingApplicationLifecycle.State state() {
        return lifecycle.state();
    }

    @Override
    public void close() {
        lifecycle.close();
    }

    /**
     * Loads the identity embedded by Maven, builds the application composition, starts it and
     * always closes it. The final stdout line remains stable for cross-platform smoke evidence.
     */
    public static void main(String[] args) {
        BuildIdentity buildIdentity = BuildIdentityLoader.load();
        TimingApplication application = TimingApplication.builder(buildIdentity).build();
        try {
            application.start();
        } finally {
            application.close();
        }

        System.out.println(smokeOutput(application.commandHandler().version(), application.state()));
    }

    static String smokeOutput(BuildIdentity buildIdentity, TimingApplicationLifecycle.State state) {
        return "event-timing-app lifecycle OK version=" + buildIdentity.version() + " state=" + state;
    }

    /** Small composition builder that creates only objects required by the current executable. */
    public static final class Builder {
        private final BuildIdentity buildIdentity;

        private Builder(BuildIdentity buildIdentity) {
            if (buildIdentity == null) {
                throw new IllegalArgumentException("buildIdentity must not be null");
            }
            this.buildIdentity = buildIdentity;
        }

        public TimingApplication build() {
            CommandHandler commandHandler = new CommandHandler(buildIdentity);
            TimingApplicationLifecycle lifecycle = new TimingApplicationLifecycle(buildIdentity);
            return new TimingApplication(commandHandler, lifecycle);
        }
    }
}

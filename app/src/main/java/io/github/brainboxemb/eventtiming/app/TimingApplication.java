package io.github.brainboxemb.eventtiming.app;

import io.github.brainboxemb.eventtiming.application.CommandHandler;
import io.github.brainboxemb.eventtiming.infra.BuildIdentity;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Executable composition root for the current SI-01 application baseline.
 *
 * <p>The class performs composition/lifecycle work and exposes the shared application boundary
 * needed by presentation adapters. Timing-domain behaviour belongs in the reusable framework;
 * later transports and I/O integrations are wired here rather than represented by layer-marker
 * objects.</p>
 */
public final class TimingApplication implements AutoCloseable {
    private static final String BUILD_IDENTITY_RESOURCE = "/event-timing-build.properties";

    private final BuildIdentity buildIdentity;
    private final CommandHandler commandHandler;
    private final TimingApplicationLifecycle lifecycle;

    private TimingApplication(
            BuildIdentity buildIdentity,
            CommandHandler commandHandler,
            TimingApplicationLifecycle lifecycle) {
        this.buildIdentity = buildIdentity;
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

    BuildIdentity buildIdentity() {
        return buildIdentity;
    }

    TimingApplicationLifecycle.State state() {
        return lifecycle.state();
    }

    @Override
    public void close() {
        lifecycle.close();
    }

    /**
     * Obtains the identity embedded by Maven, builds the application composition, starts it and
     * always closes it. The final stdout line remains stable for cross-platform smoke evidence.
     */
    public static void main(String[] args) {
        TimingApplication application = TimingApplication.builder(embeddedBuildIdentity()).build();
        try {
            application.start();
        } finally {
            application.close();
        }

        System.out.println(smokeOutput(application.buildIdentity(), application.state()));
    }

    /**
     * Reads the build metadata embedded in this executable artifact.
     *
     * <p>This is bootstrap detail rather than an application/framework service: deployment
     * configuration is loaded separately once the Step-3 configuration slice is implemented.</p>
     */
    static BuildIdentity embeddedBuildIdentity() {
        Properties properties = new Properties();
        try (InputStream input =
                TimingApplication.class.getResourceAsStream(BUILD_IDENTITY_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException(
                        "Missing build identity resource: " + BUILD_IDENTITY_RESOURCE);
            }
            properties.load(input);
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Unable to load build identity resource: " + BUILD_IDENTITY_RESOURCE, ex);
        }

        return BuildIdentity.firstApiVersion(
                properties.getProperty("application.name"),
                properties.getProperty("application.version"),
                properties.getProperty("build.revision"),
                properties.getProperty("build.timestamp"));
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
            return new TimingApplication(buildIdentity, commandHandler, lifecycle);
        }
    }
}

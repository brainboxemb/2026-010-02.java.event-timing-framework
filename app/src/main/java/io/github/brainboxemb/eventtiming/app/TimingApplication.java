package io.github.brainboxemb.eventtiming.app;

import io.github.brainboxemb.eventtiming.application.ApplicationStatus;
import io.github.brainboxemb.eventtiming.application.CommandHandler;
import io.github.brainboxemb.eventtiming.domain.timing.TimingNode;
import io.github.brainboxemb.eventtiming.infra.BuildIdentity;
import io.github.brainboxemb.eventtiming.presentation.console.LocalConsole;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        lifecycle.start();
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

    void awaitStopped() throws InterruptedException {
        lifecycle.awaitStopped();
    }

    @Override
    public void close() {
        lifecycle.close();
    }

    /**
     * Starts the application from the supplied configuration file.
     *
     * <p>The temporary no-argument startup is retained for the existing build smoke test.</p>
     */
    public static void main(String[] args) {
        BuildIdentity buildIdentity = embeddedBuildIdentity();

        if (args.length == 0) {
            runArtifactSmoke(buildIdentity);
            return;
        }
        if (args.length != 1) {
            throw new IllegalArgumentException(
                    "Usage: java -jar event-timing-app-<version>.jar <application.yml>");
        }

        TimingApplication application;
        try {
            application = configured(buildIdentity, Paths.get(args[0]));
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load application configuration: " + args[0], ex);
        }

        Runtime runtime = Runtime.getRuntime();
        Thread shutdownHook = new Thread(application::close, "event-timing-shutdown");
        runtime.addShutdownHook(shutdownHook);

        try {
            application.start();
            startLocalConsole(application);
            application.awaitStopped();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } finally {
            application.close();
            removeShutdownHook(runtime, shutdownHook);
        }

        System.out.println(smokeOutput(application.buildIdentity(), application.state()));
    }

    private static void startLocalConsole(TimingApplication application) {
        LocalConsole console = new LocalConsole(
                application.commandHandler(),
                application::close,
                new InputStreamReader(System.in),
                new OutputStreamWriter(System.out));
        Thread consoleThread = new Thread(console, "event-timing-console");
        consoleThread.setDaemon(true);
        consoleThread.start();
    }

    private static void removeShutdownHook(Runtime runtime, Thread shutdownHook) {
        try {
            runtime.removeShutdownHook(shutdownHook);
        } catch (IllegalStateException ignored) {
            // JVM shutdown is already in progress.
        }
    }

    static TimingApplication configured(BuildIdentity buildIdentity, Path configPath)
            throws IOException {
        ApplicationConfig config = ApplicationConfigLoader.load(configPath);
        TimingNode timingNode = new TimingNode(config.timingNodeId());
        return TimingApplication.builder(buildIdentity).timingNode(timingNode).build();
    }

    private static void runArtifactSmoke(BuildIdentity buildIdentity) {
        TimingApplicationLifecycle lifecycle = new TimingApplicationLifecycle(buildIdentity);
        try {
            lifecycle.start();
        } finally {
            lifecycle.close();
        }
        System.out.println(smokeOutput(buildIdentity, lifecycle.state()));
    }

    /** Reads the build metadata embedded in this executable artifact. */
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
                    () -> new ApplicationStatus(
                            lifecycle.state().name(),
                            timingNode.timingNodeId()));
            return new TimingApplication(buildIdentity, timingNode, commandHandler, lifecycle);
        }
    }
}

package io.github.brainboxemb.eventtiming.timingpoint.app;

import io.github.brainboxemb.eventtiming.timingpoint.infra.BuildIdentity;
import io.github.brainboxemb.eventtiming.timingpoint.infra.EmbeddedBuildIdentityLoader;
import io.github.brainboxemb.eventtiming.timingpoint.runtime.config.YamlLoader;
import io.github.brainboxemb.eventtiming.timingpoint.infra.logging.Logging;
import io.github.brainboxemb.eventtiming.timingpoint.infra.loggingserver.LoggingServer;
import io.github.brainboxemb.eventtiming.timingpoint.runtime.Application;
import io.github.brainboxemb.eventtiming.timingpoint.runtime.Composition;
import io.github.brainboxemb.eventtiming.timingpoint.runtime.Lifecycle;
import io.github.brainboxemb.eventtiming.timingpoint.runtime.config.Config;

import java.io.IOException;
import java.nio.file.Paths;

/** Thin executable launcher for the reusable SI-01 runtime. */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        BuildIdentity buildIdentity = EmbeddedBuildIdentityLoader.load();

        if (args.length == 0) {
            runArtifactSmoke(buildIdentity);
            return;
        }
        if (args.length != 1) {
            throw new IllegalArgumentException(
                    "Usage: java -jar timing-point-app-<version>.jar <application.yml>");
        }

        try {
            Config config = YamlLoader.load(Paths.get(args[0]));
            Logging logging = null;
            LoggingServer loggingServer = null;
            try {
                if (config.logging() != null) {
                    logging = Logging.start(config.logging());
                }
                if (config.loggingServer() != null) {
                    if (logging == null) {
                        throw new IllegalStateException(
                                "LoggingServer requires the Logging component");
                    }
                    loggingServer = new LoggingServer(config.loggingServer(), logging);
                    loggingServer.start();
                }
                Composition.run(buildIdentity, config);
            } finally {
                if (loggingServer != null) {
                    loggingServer.close();
                }
                if (logging != null) {
                    logging.close();
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Unable to start application from configuration: " + args[0],
                    ex);
        }
    }

    private static void runArtifactSmoke(BuildIdentity buildIdentity) {
        Lifecycle lifecycle = new Lifecycle(buildIdentity);
        try {
            lifecycle.start();
        } finally {
            lifecycle.close();
        }
        System.out.println(Application.smokeOutput(buildIdentity, lifecycle.state()));
    }
}

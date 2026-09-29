package io.github.brainboxemb.eventtiming.app;

import io.github.brainboxemb.eventtiming.app.bootstrap.EmbeddedBuildIdentityLoader;
import io.github.brainboxemb.eventtiming.app.bootstrap.YamlApplicationConfigLoader;
import io.github.brainboxemb.eventtiming.infra.logging.Logging;
import io.github.brainboxemb.eventtiming.infra.BuildIdentity;
import io.github.brainboxemb.eventtiming.infra.bootstrap.ApplicationBootstrap;
import io.github.brainboxemb.eventtiming.infra.bootstrap.config.ApplicationConfig;
import io.github.brainboxemb.eventtiming.runtime.TimingApplication;
import io.github.brainboxemb.eventtiming.runtime.TimingApplicationLifecycle;

import java.io.IOException;
import java.nio.file.Paths;

/** Thin default executable launcher for the reusable SI-01 framework runtime. */
public final class TimingApplicationMain {
    private TimingApplicationMain() {
    }

    public static void main(String[] args) {
        BuildIdentity buildIdentity = EmbeddedBuildIdentityLoader.load();

        if (args.length == 0) {
            runArtifactSmoke(buildIdentity);
            return;
        }
        if (args.length != 1) {
            throw new IllegalArgumentException(
                    "Usage: java -jar event-timing-app-<version>.jar <application.yml>");
        }

        try {
            ApplicationConfig config =
                    YamlApplicationConfigLoader.load(Paths.get(args[0]));
            Logging logging = null;
            try {
                if (config.logging() != null) {
                    logging = Logging.start(config.logging());
                }
                ApplicationBootstrap.run(buildIdentity, config);
            } finally {
                if (logging != null) {
                    logging.close();
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Unable to bootstrap application from configuration: " + args[0],
                    ex);
        }
    }

    private static void runArtifactSmoke(BuildIdentity buildIdentity) {
        TimingApplicationLifecycle lifecycle = new TimingApplicationLifecycle(buildIdentity);
        try {
            lifecycle.start();
        } finally {
            lifecycle.close();
        }
        System.out.println(TimingApplication.smokeOutput(buildIdentity, lifecycle.state()));
    }
}

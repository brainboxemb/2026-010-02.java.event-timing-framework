package io.github.brainboxemb.eventtiming.app.bootstrap;

import io.github.brainboxemb.eventtiming.app.TimingApplicationMain;
import io.github.brainboxemb.eventtiming.infra.BuildIdentity;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Maps the executable's filtered build resource into the framework BuildIdentity value. */
public final class EmbeddedBuildIdentityLoader {
    private static final String RESOURCE = "/event-timing-build.properties";

    private EmbeddedBuildIdentityLoader() {
    }

    public static BuildIdentity load() {
        Properties properties = new Properties();
        try (InputStream input = TimingApplicationMain.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing build identity resource: " + RESOURCE);
            }
            properties.load(input);
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Unable to load build identity resource: " + RESOURCE,
                    ex);
        }

        return BuildIdentity.firstApiVersion(
                properties.getProperty("application.name"),
                properties.getProperty("application.version"),
                properties.getProperty("build.revision"),
                properties.getProperty("build.sourceRef"),
                properties.getProperty("build.origin"),
                parseDirty(properties.getProperty("build.dirty")));
    }

    private static boolean parseDirty(String value) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalStateException("Invalid embedded build.dirty value: " + value);
    }
}

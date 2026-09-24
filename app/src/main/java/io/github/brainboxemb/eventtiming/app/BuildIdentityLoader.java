package io.github.brainboxemb.eventtiming.app;

import io.github.brainboxemb.eventtiming.infra.BuildIdentity;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Loads executable build metadata and maps it into the shared application identity value. */
final class BuildIdentityLoader {
    private static final String RESOURCE = "/event-timing-build.properties";

    private BuildIdentityLoader() {
    }

    static BuildIdentity load() {
        Properties properties = new Properties();
        try (InputStream input = BuildIdentityLoader.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing build identity resource: " + RESOURCE);
            }
            properties.load(input);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load build identity resource: " + RESOURCE, ex);
        }

        return BuildIdentity.firstApiVersion(
                properties.getProperty("application.name"),
                properties.getProperty("application.version"),
                properties.getProperty("build.revision"),
                properties.getProperty("build.timestamp"));
    }
}

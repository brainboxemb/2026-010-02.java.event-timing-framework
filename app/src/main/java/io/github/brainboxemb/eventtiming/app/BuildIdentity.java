package io.github.brainboxemb.eventtiming.app;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Build identity embedded in the executable application artifact. */
public final class BuildIdentity {
    private static final String RESOURCE = "/event-timing-build.properties";

    private final String applicationName;
    private final String version;

    BuildIdentity(String applicationName, String version) {
        this.applicationName = requireText(applicationName, "applicationName");
        this.version = requireText(version, "version");
    }

    public static BuildIdentity load() {
        Properties properties = new Properties();
        try (InputStream input = BuildIdentity.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing build identity resource: " + RESOURCE);
            }
            properties.load(input);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load build identity resource: " + RESOURCE, ex);
        }

        return new BuildIdentity(
                properties.getProperty("application.name"),
                properties.getProperty("application.version"));
    }

    public String applicationName() {
        return applicationName;
    }

    public String version() {
        return version;
    }

    public String displayName() {
        return applicationName + " " + version;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}

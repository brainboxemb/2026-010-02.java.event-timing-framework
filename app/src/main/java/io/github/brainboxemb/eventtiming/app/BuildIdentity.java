package io.github.brainboxemb.eventtiming.app;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Immutable identity of the executable application artifact that is currently running.
 *
 * <p>The software version comes from Maven {@code ${project.version}}. The concrete build
 * provenance is captured separately as the source revision and UTC build timestamp. Keeping
 * those concepts separate means many snapshot builds can share the same Maven version while
 * remaining individually identifiable.</p>
 *
 * <p>The values are embedded during the Maven build in {@code event-timing-build.properties}.
 * The owning design is coordinated in the meta repository, especially
 * {@code docs/31-01-SDD-02-java-component-design.md} and the SI-01 version/status requirements.</p>
 */
public final class BuildIdentity {
    private static final String RESOURCE = "/event-timing-build.properties";

    private final String applicationName;
    private final String version;
    private final String revision;
    private final String buildTimestamp;

    BuildIdentity(String applicationName, String version, String revision, String buildTimestamp) {
        this.applicationName = requireText(applicationName, "applicationName");
        this.version = requireText(version, "version");
        this.revision = requireText(revision, "revision");
        this.buildTimestamp = requireText(buildTimestamp, "buildTimestamp");
    }

    /**
     * Loads the identity embedded in this built application artifact.
     *
     * <p>This method deliberately does not inspect Git at runtime. The identity describes the
     * artifact that was built, so the values are resolved once by Maven and packaged into the
     * JAR.</p>
     */
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
                properties.getProperty("application.version"),
                properties.getProperty("build.revision"),
                properties.getProperty("build.timestamp"));
    }

    public String applicationName() {
        return applicationName;
    }

    /** Returns the Maven project version that identifies the software release/snapshot line. */
    public String version() {
        return version;
    }

    /** Returns the source-control revision captured when this artifact was built. */
    public String revision() {
        return revision;
    }

    /** Returns the UTC build timestamp captured when this artifact was built. */
    public String buildTimestamp() {
        return buildTimestamp;
    }

    /** Returns the stable human-readable product/version label without build-specific data. */
    public String displayName() {
        return applicationName + " " + version;
    }

    /** Returns concise provenance that distinguishes concrete builds of the same snapshot. */
    public String provenance() {
        return "revision=" + revision + " built=" + buildTimestamp;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}

package io.github.brainboxemb.eventtiming.infra;

/**
 * Immutable identity of one built application artifact.
 *
 * <p>This is build/runtime provenance infrastructure. Executable composition loads these values
 * from the built artifact and application-facing code may expose them to clients without becoming
 * the owner of the provenance itself.</p>
 *
 * <p>Field semantics follow the first-executable IF-03 contract in the meta repository
 * {@code docs/40-01-IDD-application-control-status.md}.</p>
 */
public final class BuildIdentity {
    /** API major version shared by every first-executable representation of this build. */
    public static final String FIRST_API_VERSION = "1";

    private final String application;
    private final String version;
    private final String revision;
    private final String buildTime;
    private final String apiVersion;

    public BuildIdentity(
            String application,
            String version,
            String revision,
            String buildTime,
            String apiVersion) {
        this.application = requireText(application, "application");
        this.version = requireText(version, "version");
        this.revision = requireText(revision, "revision");
        this.buildTime = requireText(buildTime, "buildTime");
        this.apiVersion = requireText(apiVersion, "apiVersion");
    }

    public static BuildIdentity firstApiVersion(
            String application,
            String version,
            String revision,
            String buildTime) {
        return new BuildIdentity(application, version, revision, buildTime, FIRST_API_VERSION);
    }

    public String application() {
        return application;
    }

    public String version() {
        return version;
    }

    public String revision() {
        return revision;
    }

    public String buildTime() {
        return buildTime;
    }

    public String apiVersion() {
        return apiVersion;
    }

    public String displayName() {
        return application + " " + version;
    }

    public String provenance() {
        return "revision=" + revision + " built=" + buildTime;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}

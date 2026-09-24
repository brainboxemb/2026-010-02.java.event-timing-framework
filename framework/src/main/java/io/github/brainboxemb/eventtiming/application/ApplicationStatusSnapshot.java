package io.github.brainboxemb.eventtiming.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable coherent snapshot of the shared SI-01 application status semantics.
 *
 * <p>Presentation adapters may map this value to console text, JSON or events, but they must not
 * mutate it or replace it with transport-owned state. The snapshot shape follows the first
 * executable status contract in {@code docs/40-01-IDD-application-control-status.md}.</p>
 */
public final class ApplicationStatusSnapshot {
    private final BuildIdentity buildIdentity;
    private final ApplicationState applicationState;
    private final Instant startedAt;
    private final List<WaypointStatus> waypoints;
    private final List<ApplicationProblem> problems;

    public ApplicationStatusSnapshot(
            BuildIdentity buildIdentity,
            ApplicationState applicationState,
            Instant startedAt,
            List<WaypointStatus> waypoints,
            List<ApplicationProblem> problems) {
        if (buildIdentity == null) {
            throw new IllegalArgumentException("buildIdentity must not be null");
        }
        if (applicationState == null) {
            throw new IllegalArgumentException("applicationState must not be null");
        }
        if (startedAt == null) {
            throw new IllegalArgumentException("startedAt must not be null");
        }
        this.buildIdentity = buildIdentity;
        this.applicationState = applicationState;
        this.startedAt = startedAt;
        this.waypoints = immutableCopy(waypoints, "waypoints");
        this.problems = immutableCopy(problems, "problems");
    }

    /** Returns the API major version from the same authoritative build identity. */
    public String apiVersion() {
        return buildIdentity.apiVersion();
    }

    public BuildIdentity buildIdentity() {
        return buildIdentity;
    }

    public ApplicationState applicationState() {
        return applicationState;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public List<WaypointStatus> waypoints() {
        return waypoints;
    }

    public List<ApplicationProblem> problems() {
        return problems;
    }

    private static <T> List<T> immutableCopy(List<T> values, String field) {
        if (values == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        List<T> copy = new ArrayList<T>(values.size());
        for (T value : values) {
            if (value == null) {
                throw new IllegalArgumentException(field + " must not contain null elements");
            }
            copy.add(value);
        }
        return Collections.unmodifiableList(copy);
    }
}

package io.github.brainboxemb.eventtiming.application;

/** Immutable application-level status view of one configured Waypoint. */
public final class WaypointStatus {
    private final String uniqueId;
    private final WaypointLifecycle lifecycle;

    public WaypointStatus(String uniqueId, WaypointLifecycle lifecycle) {
        if (uniqueId == null || uniqueId.trim().isEmpty()) {
            throw new IllegalArgumentException("uniqueId must not be blank");
        }
        if (lifecycle == null) {
            throw new IllegalArgumentException("lifecycle must not be null");
        }
        this.uniqueId = uniqueId;
        this.lifecycle = lifecycle;
    }

    /** Stable logical identity of the configured Waypoint. */
    public String uniqueId() {
        return uniqueId;
    }

    public WaypointLifecycle lifecycle() {
        return lifecycle;
    }
}

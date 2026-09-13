package io.github.brainboxemb.eventtiming.application;

/**
 * Application-owned authority for the current immutable status snapshot.
 *
 * <p>The model deliberately contains no transport, lifecycle-host or configuration behaviour.
 * Later Step-3 activities may replace the current snapshot as runtime state changes, while every
 * adapter reads the same model. Requiring snapshots to reuse the exact build-identity instance
 * prevents an interface or subsystem from silently creating another authoritative version value.</p>
 */
public final class ApplicationStatusModel {
    private final BuildIdentity buildIdentity;
    private volatile ApplicationStatusSnapshot currentStatus;

    public ApplicationStatusModel(BuildIdentity buildIdentity, ApplicationStatusSnapshot initialStatus) {
        if (buildIdentity == null) {
            throw new IllegalArgumentException("buildIdentity must not be null");
        }
        requireSharedIdentity(buildIdentity, initialStatus);
        this.buildIdentity = buildIdentity;
        this.currentStatus = initialStatus;
    }

    /** Returns the one build identity shared by version and status queries. */
    public BuildIdentity buildIdentity() {
        return buildIdentity;
    }

    /** Returns one immutable coherent current status snapshot. */
    public ApplicationStatusSnapshot currentStatus() {
        return currentStatus;
    }

    /**
     * Atomically replaces the current immutable snapshot while preserving build identity ownership.
     * Runtime/lifecycle activities decide when a replacement is appropriate.
     */
    public synchronized void replaceStatus(ApplicationStatusSnapshot nextStatus) {
        requireSharedIdentity(buildIdentity, nextStatus);
        currentStatus = nextStatus;
    }

    private static void requireSharedIdentity(
            BuildIdentity buildIdentity,
            ApplicationStatusSnapshot status) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (status.buildIdentity() != buildIdentity) {
            throw new IllegalArgumentException(
                    "status must reuse the authoritative buildIdentity instance");
        }
    }
}

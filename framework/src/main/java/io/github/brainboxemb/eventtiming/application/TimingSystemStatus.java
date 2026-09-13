package io.github.brainboxemb.eventtiming.application;

/** Immutable application-level status view of one configured timing-system instance. */
public final class TimingSystemStatus {
    private final String id;
    private final TimingSystemLifecycle lifecycle;

    public TimingSystemStatus(String id, TimingSystemLifecycle lifecycle) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (lifecycle == null) {
            throw new IllegalArgumentException("lifecycle must not be null");
        }
        this.id = id;
        this.lifecycle = lifecycle;
    }

    public String id() {
        return id;
    }

    public TimingSystemLifecycle lifecycle() {
        return lifecycle;
    }
}

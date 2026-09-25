package io.github.brainboxemb.eventtiming.application;

import io.github.brainboxemb.eventtiming.domain.timing.TimingNode;
import io.github.brainboxemb.eventtiming.domain.timing.TimingNodeId;

import java.time.Instant;

/** Transport-independent current status used by presentation adapters. */
public final class ApplicationStatus {
    private final String applicationState;
    private final Instant startedAt;
    private final TimingNodeId timingNodeId;
    private final TimingNode.Lifecycle timingNodeLifecycle;

    public ApplicationStatus(
            String applicationState,
            Instant startedAt,
            TimingNodeId timingNodeId,
            TimingNode.Lifecycle timingNodeLifecycle) {
        if (applicationState == null || applicationState.trim().isEmpty()) {
            throw new IllegalArgumentException("applicationState must not be blank");
        }
        if (startedAt == null) {
            throw new IllegalArgumentException("startedAt must not be null");
        }
        if (timingNodeId == null) {
            throw new IllegalArgumentException("timingNodeId must not be null");
        }
        if (timingNodeLifecycle == null) {
            throw new IllegalArgumentException("timingNodeLifecycle must not be null");
        }
        this.applicationState = applicationState;
        this.startedAt = startedAt;
        this.timingNodeId = timingNodeId;
        this.timingNodeLifecycle = timingNodeLifecycle;
    }

    public String applicationState() {
        return applicationState;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public TimingNodeId timingNodeId() {
        return timingNodeId;
    }

    public TimingNode.Lifecycle timingNodeLifecycle() {
        return timingNodeLifecycle;
    }
}

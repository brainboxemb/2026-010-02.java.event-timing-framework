package io.github.brainboxemb.eventtiming.application;

import io.github.brainboxemb.eventtiming.domain.timing.TimingNodeId;

/** Minimal transport-independent application status used by current presentation adapters. */
public final class ApplicationStatus {
    private final String applicationState;
    private final TimingNodeId timingNodeId;

    public ApplicationStatus(String applicationState, TimingNodeId timingNodeId) {
        if (applicationState == null || applicationState.trim().isEmpty()) {
            throw new IllegalArgumentException("applicationState must not be blank");
        }
        if (timingNodeId == null) {
            throw new IllegalArgumentException("timingNodeId must not be null");
        }
        this.applicationState = applicationState;
        this.timingNodeId = timingNodeId;
    }

    public String applicationState() {
        return applicationState;
    }

    public TimingNodeId timingNodeId() {
        return timingNodeId;
    }
}

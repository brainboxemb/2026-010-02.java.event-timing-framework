package io.github.brainboxemb.eventtiming.timingpoint.application;

import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNode;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeId;

/** Transport-independent current TimingNode status used by presentation adapters. */
public final class ApplicationStatus {
    private final TimingNodeId timingNodeId;
    private final TimingNode.Lifecycle timingNodeLifecycle;

    public ApplicationStatus(
            TimingNodeId timingNodeId,
            TimingNode.Lifecycle timingNodeLifecycle) {
        if (timingNodeId == null) {
            throw new IllegalArgumentException("timingNodeId must not be null");
        }
        if (timingNodeLifecycle == null) {
            throw new IllegalArgumentException("timingNodeLifecycle must not be null");
        }
        this.timingNodeId = timingNodeId;
        this.timingNodeLifecycle = timingNodeLifecycle;
    }

    public TimingNodeId timingNodeId() {
        return timingNodeId;
    }

    public TimingNode.Lifecycle timingNodeLifecycle() {
        return timingNodeLifecycle;
    }
}

package io.github.brainboxemb.eventtiming.app;

import io.github.brainboxemb.eventtiming.domain.timing.TimingNodeId;

/** Effective deployment configuration after parsing and validation. */
final class ApplicationConfig {
    private final TimingNodeId timingNodeId;

    ApplicationConfig(TimingNodeId timingNodeId) {
        if (timingNodeId == null) {
            throw new IllegalArgumentException("timingNodeId must not be null");
        }
        this.timingNodeId = timingNodeId;
    }

    TimingNodeId timingNodeId() {
        return timingNodeId;
    }
}

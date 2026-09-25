package io.github.brainboxemb.eventtiming.app;

import io.github.brainboxemb.eventtiming.domain.timing.TimingNodeId;

/** Effective deployment configuration after parsing and validation. */
final class ApplicationConfig {
    private final TimingNodeId timingNodeId;
    private final PresentationConfig presentation;

    ApplicationConfig(TimingNodeId timingNodeId, PresentationConfig presentation) {
        if (timingNodeId == null) {
            throw new IllegalArgumentException("timingNodeId must not be null");
        }
        if (presentation == null) {
            throw new IllegalArgumentException("presentation must not be null");
        }
        this.timingNodeId = timingNodeId;
        this.presentation = presentation;
    }

    TimingNodeId timingNodeId() {
        return timingNodeId;
    }

    PresentationConfig presentation() {
        return presentation;
    }
}

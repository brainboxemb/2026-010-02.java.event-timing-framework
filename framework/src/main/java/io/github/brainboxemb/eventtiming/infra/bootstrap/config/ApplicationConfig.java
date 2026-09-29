package io.github.brainboxemb.eventtiming.infra.bootstrap.config;

import io.github.brainboxemb.eventtiming.domain.timing.TimingNodeId;

/** Effective deployment configuration consumed by the framework bootstrap. */
public final class ApplicationConfig {
    private final TimingNodeId timingNodeId;
    private final PresentationConfig presentation;
    private final LoggingConfig logging;

    public ApplicationConfig(TimingNodeId timingNodeId, PresentationConfig presentation) {
        this(timingNodeId, presentation, null);
    }

    public ApplicationConfig(
            TimingNodeId timingNodeId,
            PresentationConfig presentation,
            LoggingConfig logging) {
        if (timingNodeId == null) {
            throw new IllegalArgumentException("timingNodeId must not be null");
        }
        if (presentation == null) {
            throw new IllegalArgumentException("presentation must not be null");
        }
        this.timingNodeId = timingNodeId;
        this.presentation = presentation;
        this.logging = logging;
    }

    public TimingNodeId timingNodeId() {
        return timingNodeId;
    }

    public PresentationConfig presentation() {
        return presentation;
    }

    public LoggingConfig logging() {
        return logging;
    }
}

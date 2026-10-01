package io.github.brainboxemb.eventtiming.timingpoint.infra.bootstrap.config;

import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeId;
import io.github.brainboxemb.eventtiming.timingpoint.infra.logging.LoggingConfig;
import io.github.brainboxemb.eventtiming.timingpoint.infra.loggingserver.LoggingServerConfig;

/** Effective deployment configuration consumed by the framework bootstrap. */
public final class ApplicationConfig {
    private final TimingNodeId timingNodeId;
    private final PresentationConfig presentation;
    private final LoggingConfig logging;
    private final LoggingServerConfig loggingServer;

    public ApplicationConfig(TimingNodeId timingNodeId, PresentationConfig presentation) {
        this(timingNodeId, presentation, null, null);
    }

    public ApplicationConfig(
            TimingNodeId timingNodeId,
            PresentationConfig presentation,
            LoggingConfig logging) {
        this(timingNodeId, presentation, logging, null);
    }

    public ApplicationConfig(
            TimingNodeId timingNodeId,
            PresentationConfig presentation,
            LoggingConfig logging,
            LoggingServerConfig loggingServer) {
        if (timingNodeId == null) {
            throw new IllegalArgumentException("timingNodeId must not be null");
        }
        if (presentation == null) {
            throw new IllegalArgumentException("presentation must not be null");
        }
        this.timingNodeId = timingNodeId;
        this.presentation = presentation;
        this.logging = logging;
        this.loggingServer = loggingServer;
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

    public LoggingServerConfig loggingServer() {
        return loggingServer;
    }
}

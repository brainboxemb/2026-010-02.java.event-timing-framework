package io.github.brainboxemb.eventtiming.timingpoint.infra.bootstrap.config;

import io.github.brainboxemb.eventtiming.timingdata.TimingNodeId;
import io.github.brainboxemb.eventtiming.timingpoint.infra.logging.LoggingConfig;
import io.github.brainboxemb.eventtiming.timingpoint.infra.loggingserver.LoggingServerConfig;

import java.nio.file.Path;

/** Effective deployment configuration consumed by the application-core bootstrap. */
public final class ApplicationConfig {
    private final TimingNodeId timingNodeId;
    private final PresentationConfig presentation;
    private final LoggingConfig logging;
    private final LoggingServerConfig loggingServer;
    private final Path timingDataPath;

    public ApplicationConfig(TimingNodeId timingNodeId, PresentationConfig presentation) {
        this(timingNodeId, presentation, null, null, null);
    }

    public ApplicationConfig(
            TimingNodeId timingNodeId,
            PresentationConfig presentation,
            LoggingConfig logging) {
        this(timingNodeId, presentation, logging, null, null);
    }

    public ApplicationConfig(
            TimingNodeId timingNodeId,
            PresentationConfig presentation,
            LoggingConfig logging,
            LoggingServerConfig loggingServer) {
        this(timingNodeId, presentation, logging, loggingServer, null);
    }

    /**
     * Creates the effective executable configuration.
     *
     * <p>The TimingData path is deployment/composition state. It selects the
     * authoritative file used by the current single-TimingNode reference
     * composition; record format and recovery semantics remain owned by the
     * TimingData store.</p>
     */
    public ApplicationConfig(
            TimingNodeId timingNodeId,
            PresentationConfig presentation,
            LoggingConfig logging,
            LoggingServerConfig loggingServer,
            Path timingDataPath) {
        if (timingNodeId == null) {
            throw new IllegalArgumentException("timingNodeId must not be null");
        }
        if (presentation == null) {
            throw new IllegalArgumentException("presentation must not be null");
        }
        if (timingDataPath != null && timingDataPath.toString().trim().isEmpty()) {
            throw new IllegalArgumentException("timingDataPath must not be empty");
        }
        this.timingNodeId = timingNodeId;
        this.presentation = presentation;
        this.logging = logging;
        this.loggingServer = loggingServer;
        this.timingDataPath = timingDataPath;
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

    public Path timingDataPath() {
        return timingDataPath;
    }
}

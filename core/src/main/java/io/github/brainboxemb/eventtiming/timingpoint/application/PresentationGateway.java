package io.github.brainboxemb.eventtiming.timingpoint.application;

import io.github.brainboxemb.eventtiming.timingdata.TimingDataTypes.LocationId;
import io.github.brainboxemb.eventtiming.timingdata.TimingDataTypes.RegistrationId;
import io.github.brainboxemb.eventtiming.timingdata.TimingData;
import io.github.brainboxemb.eventtiming.timingdata.TimingTimestamp;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNode;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeCommands;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeQueries;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeTypes.CloseResult;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeTypes.OpenResult;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeTypes.RegistrationResult;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeTypes.SetLocationResult;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeTypes.Status;
import io.github.brainboxemb.eventtiming.timingpoint.infra.BuildIdentity;
import io.github.brainboxemb.eventtiming.timingpoint.platform.events.Event;
import io.github.brainboxemb.eventtiming.timingpoint.platform.events.EventSource;

import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared transport-independent application gateway for Presentation.
 *
 * <p>This class is intentionally small. It is not a command bus, mediator framework or generic
 * message registry. Presentation adapters use these methods instead of calling TimingNode
 * directly, so HTTP, WebSocket, terminal and Engineering Client paths share one application
 * gateway. State-changing operations map to typed TimingNode commands, consistency-sensitive
 * reads map to typed TimingNode queries, and presentation-facing application metadata,
 * capabilities and event sources are exposed through the same gateway.</p>
 */
public final class PresentationGateway {
    private static final Logger LOG = LoggerFactory.getLogger(PresentationGateway.class);
    /** First Step-4 engineering capability set. */
    public static final class Capabilities {
        private final boolean directRegistrationSimulationSupported;
        private final boolean directRegistrationSimulationEnabled;

        private Capabilities(
                boolean directRegistrationSimulationSupported,
                boolean directRegistrationSimulationEnabled) {
            this.directRegistrationSimulationSupported =
                    directRegistrationSimulationSupported;
            this.directRegistrationSimulationEnabled =
                    directRegistrationSimulationEnabled;
        }

        public boolean directRegistrationSimulationSupported() {
            return directRegistrationSimulationSupported;
        }

        public boolean directRegistrationSimulationEnabled() {
            return directRegistrationSimulationEnabled;
        }
    }

    private static final Capabilities STEP4_CAPABILITIES =
            new Capabilities(true, true);

    private final BuildIdentity buildIdentity;
    private final TimingNode timingNode;
    private final Event<ApplicationStatus> statusChangedEvent = new Event<>();

    /**
     * Creates the presentation-facing application gateway for one fully composed TimingNode.
     */
    public PresentationGateway(BuildIdentity buildIdentity, TimingNode timingNode) {
        if (buildIdentity == null) {
            throw new IllegalArgumentException("buildIdentity must not be null");
        }
        if (timingNode == null) {
            throw new IllegalArgumentException("timingNode must not be null");
        }
        this.buildIdentity = buildIdentity;
        this.timingNode = timingNode;
        timingNode.statusChanged().subscribe(this::updateStatus);
    }

    /** Returns the authoritative application build/version identity. */
    public BuildIdentity version() {
        return buildIdentity;
    }

    /** Returns the current authoritative TimingNode status for presentation adapters. */
    public ApplicationStatus status() {
        return applicationStatus(
                timingNode.query(TimingNodeQueries.status()));
    }

    /** Returns the first Step-4 engineering capabilities for the current composition. */
    public Capabilities capabilities() {
        return STEP4_CAPABILITIES;
    }

    /** Sets the current operational LocationId through the TimingNode serial owner. */
    public SetLocationResult setLocation(LocationId locationId) {
        return timingNode.invoke(TimingNodeCommands.setLocation(locationId));
    }

    /** Opens registration through the TimingNode serial owner. */
    public OpenResult open() {
        return timingNode.invoke(TimingNodeCommands.open());
    }

    /** Closes registration through the TimingNode serial owner. */
    public CloseResult close() {
        return timingNode.invoke(TimingNodeCommands.close());
    }

    /**
     * Injects one already-accepted semantic registration.
     *
     * <p>The caller does not supply TimingNode identity, active LocationId,
     * sequence number, recordedAt or final TimingData.</p>
     */
    public RegistrationResult commitAutomaticRegistration(
            RegistrationId registrationId,
            TimingTimestamp observationTime) {
        return timingNode.invoke(
                TimingNodeCommands.commitAutomaticRegistration(
                        registrationId,
                        observationTime));
    }

    /** Returns the number of committed records in the current node LogBook. */
    public int logBookCount() {
        return timingNode.query(TimingNodeQueries.timingDataCount());
    }

    /**
     * Visits a bounded committed LogBook range in source order.
     *
     * <p>The visitor executes synchronously on the TimingNode serial lane. It
     * must remain short/non-blocking and must not call back into this TimingNode.
     * The returned value is the total committed LogBook count from the same
     * ordered read.</p>
     */
    public int visitLogBookFrom(
            long fromSequence,
            int limit,
            Consumer<TimingData> visitor) {
        return timingNode.query(
                TimingNodeQueries.visitTimingDataRange(
                        fromSequence,
                        limit,
                        visitor));
    }

    /**
     * Visits a bounded newest LogBook range in source order.
     *
     * @return total committed LogBook count from the same ordered read
     */
    public int visitLatestLogBook(
            int limit,
            Consumer<TimingData> visitor) {
        return timingNode.query(
                TimingNodeQueries.visitLatestTimingData(limit, visitor));
    }

    /** Returns the subscription-only authoritative status-change event. */
    public EventSource<ApplicationStatus> statusChanged() {
        return statusChangedEvent;
    }

    /** Returns the subscription-only newly committed TimingData event. */
    public EventSource<TimingData> newTimingData() {
        return timingNode.newTimingData();
    }

    /**
     * Updates the application-facing status stream from an authoritative
     * TimingNode status change.
     *
     * <p>The TimingNode decides whether a real change occurred on its serial
     * lane. Publishing that mapped fact is an implementation detail here.</p>
     */
    private void updateStatus(Status status) {
        ApplicationStatus update = applicationStatus(status);
        Event.DeliveryReport delivery = statusChangedEvent.emit(update);
        if (!delivery.successful()) {
            LOG.warn(
                    "Application status changed but "
                            + delivery.failureCount()
                            + " status listener(s) failed for "
                            + update.timingNodeId().value(),
                    delivery.failures().get(0));
        }
    }

    private static ApplicationStatus applicationStatus(Status status) {
        return new ApplicationStatus(
                status.timingNodeId(),
                status.lifecycle(),
                status.locationId());
    }

}

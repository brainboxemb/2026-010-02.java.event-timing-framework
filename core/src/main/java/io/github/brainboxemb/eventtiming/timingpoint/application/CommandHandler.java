package io.github.brainboxemb.eventtiming.timingpoint.application;

import io.github.brainboxemb.eventtiming.timingdata.LocationId;
import io.github.brainboxemb.eventtiming.timingdata.RegistrationId;
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

import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared transport-independent application boundary for client commands and queries.
 *
 * <p>This class is intentionally small. It is not a command bus, mediator framework or generic
 * message registry. Presentation adapters use these methods instead of calling TimingNode
 * directly, so HTTP, WebSocket, terminal and Engineering Client paths share one application
 * boundary. State-changing application commands map to typed TimingNode commands;
 * application reads map to typed TimingNode queries.</p>
 */
public final class CommandHandler {
    private static final Logger LOG = LoggerFactory.getLogger(CommandHandler.class);
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
    private final Supplier<ApplicationStatus> statusSupplier;
    private final TimingNode timingNode;
    private final Event<ApplicationStatus> statusChangedEvent = new Event<>();

    /**
     * Full production/application composition.
     *
     * <p>All Step-4 state-changing operations and TimingData queries are available
     * through this form. The handler owns no duplicate mutable TimingNode state.</p>
     */
    public CommandHandler(BuildIdentity buildIdentity, TimingNode timingNode) {
        this(
                buildIdentity,
                statusSupplier(timingNode),
                requireTimingNode(timingNode));
    }

    /**
     * Status-only constructor retained for small presentation/component tests.
     *
     * <p>Control/history operations require the full TimingNode composition and
     * fail clearly when called on a status-only handler.</p>
     */
    public CommandHandler(
            BuildIdentity buildIdentity,
            Supplier<ApplicationStatus> statusSupplier) {
        this(buildIdentity, statusSupplier, null);
    }

    private CommandHandler(
            BuildIdentity buildIdentity,
            Supplier<ApplicationStatus> statusSupplier,
            TimingNode timingNode) {
        if (buildIdentity == null) {
            throw new IllegalArgumentException("buildIdentity must not be null");
        }
        if (statusSupplier == null) {
            throw new IllegalArgumentException("statusSupplier must not be null");
        }
        this.buildIdentity = buildIdentity;
        this.statusSupplier = statusSupplier;
        this.timingNode = timingNode;
        if (timingNode != null) {
            timingNode.statusChanged().subscribe(this::updateStatus);
        }
    }

    /** Returns the authoritative application build/version identity. */
    public BuildIdentity version() {
        return buildIdentity;
    }

    /** Returns the current authoritative TimingNode status for presentation adapters. */
    public ApplicationStatus status() {
        ApplicationStatus status = statusSupplier.get();
        if (status == null) {
            throw new IllegalStateException("statusSupplier returned null");
        }
        return status;
    }

    /** Returns the first Step-4 engineering capabilities for the current composition. */
    public Capabilities capabilities() {
        requireOperationalTimingNode("capabilities");
        return STEP4_CAPABILITIES;
    }

    /** Sets the current operational LocationId through the TimingNode serial owner. */
    public SetLocationResult setLocation(LocationId locationId) {
        TimingNode node = requireOperationalTimingNode("setLocation");
        return node.invoke(TimingNodeCommands.setLocation(locationId));
    }

    /** Opens registration through the TimingNode serial owner. */
    public OpenResult open() {
        TimingNode node = requireOperationalTimingNode("open");
        return node.invoke(TimingNodeCommands.open());
    }

    /** Closes registration through the TimingNode serial owner. */
    public CloseResult close() {
        TimingNode node = requireOperationalTimingNode("close");
        return node.invoke(TimingNodeCommands.close());
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
        return requireOperationalTimingNode("commitAutomaticRegistration")
                .invoke(TimingNodeCommands.commitAutomaticRegistration(
                        registrationId,
                        observationTime));
    }

    /** Returns the number of committed records in the current node LogBook. */
    public int logBookCount() {
        return requireOperationalTimingNode("logBookCount")
                .query(TimingNodeQueries.timingDataCount());
    }

    /** Returns a bounded committed LogBook range starting at an inclusive sequence. */
    public List<TimingData> logBookFrom(long fromSequence, int limit) {
        return requireOperationalTimingNode("logBookFrom")
                .query(TimingNodeQueries.timingDataRange(fromSequence, limit));
    }

    /** Returns a bounded newest LogBook range in committed source order. */
    public List<TimingData> latestLogBook(int limit) {
        return requireOperationalTimingNode("latestLogBook")
                .query(TimingNodeQueries.latestTimingData(limit));
    }

    /** Returns the subscription-only authoritative status-change event. */
    public EventSource<ApplicationStatus> statusChanged() {
        return statusChangedEvent;
    }

    /** Returns the subscription-only newly committed TimingData event. */
    public EventSource<TimingData> newTimingData() {
        return requireOperationalTimingNode("newTimingData").newTimingData();
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

    private TimingNode requireOperationalTimingNode(String operation) {
        if (timingNode == null) {
            throw new IllegalStateException(
                    operation + " requires a fully composed TimingNode");
        }
        return timingNode;
    }

    private static Supplier<ApplicationStatus> statusSupplier(TimingNode timingNode) {
        TimingNode node = requireTimingNode(timingNode);
        return () -> applicationStatus(
                node.query(TimingNodeQueries.status()));
    }

    private static ApplicationStatus applicationStatus(Status status) {
        return new ApplicationStatus(
                status.timingNodeId(),
                status.lifecycle(),
                status.locationId());
    }

    private static TimingNode requireTimingNode(TimingNode timingNode) {
        if (timingNode == null) {
            throw new IllegalArgumentException("timingNode must not be null");
        }
        return timingNode;
    }
}

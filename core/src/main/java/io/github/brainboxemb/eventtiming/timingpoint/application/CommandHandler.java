package io.github.brainboxemb.eventtiming.timingpoint.application;

import io.github.brainboxemb.eventtiming.timingdata.LocationId;
import io.github.brainboxemb.eventtiming.timingdata.RegistrationId;
import io.github.brainboxemb.eventtiming.timingdata.TimingData;
import io.github.brainboxemb.eventtiming.timingdata.TimingTimestamp;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNode;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeCommands;
import io.github.brainboxemb.eventtiming.timingpoint.domain.logbook.LogBookVisitor;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeQueries;
import io.github.brainboxemb.eventtiming.timingpoint.infra.BuildIdentity;
import io.github.brainboxemb.eventtiming.timingpoint.platform.events.Event;

import java.util.function.Consumer;
import java.util.function.Supplier;

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
    public TimingNode.SetLocationResult setLocation(LocationId locationId) {
        TimingNode node = requireOperationalTimingNode("setLocation");
        ApplicationStatus before = status();
        TimingNode.SetLocationResult result =
                node.invoke(TimingNodeCommands.setLocation(locationId));
        publishStatusChangedWhenDifferent(before);
        return result;
    }

    /** Opens registration through the TimingNode serial owner. */
    public TimingNode.OpenResult open() {
        TimingNode node = requireOperationalTimingNode("open");
        ApplicationStatus before = status();
        TimingNode.OpenResult result = node.invoke(TimingNodeCommands.open());
        publishStatusChangedWhenDifferent(before);
        return result;
    }

    /** Closes registration through the TimingNode serial owner. */
    public TimingNode.CloseResult close() {
        TimingNode node = requireOperationalTimingNode("close");
        ApplicationStatus before = status();
        TimingNode.CloseResult result = node.invoke(TimingNodeCommands.close());
        publishStatusChangedWhenDifferent(before);
        return result;
    }

    /**
     * Injects one already-accepted semantic registration.
     *
     * <p>The caller does not supply TimingNode identity, active LocationId,
     * sequence number, recordedAt or final TimingData.</p>
     */
    public TimingNode.RegistrationResult commitAutomaticRegistration(
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

    /** Visits a bounded committed LogBook range starting at an inclusive sequence. */
    public <R> R logBookFrom(
            long fromSequence,
            int limit,
            LogBookVisitor<R> visitor) {
        return requireOperationalTimingNode("logBookFrom")
                .query(TimingNodeQueries.timingDataRange(
                        fromSequence,
                        limit,
                        visitor));
    }

    /** Visits a bounded newest LogBook range in committed source order. */
    public <R> R latestLogBook(
            int limit,
            LogBookVisitor<R> visitor) {
        return requireOperationalTimingNode("latestLogBook")
                .query(TimingNodeQueries.latestTimingData(limit, visitor));
    }

    /** Subscribes to authoritative status changes caused through this application boundary. */
    public boolean subscribeStatusChanged(Consumer<ApplicationStatus> listener) {
        return statusChangedEvent.subscribe(listener);
    }

    public boolean unsubscribeStatusChanged(Consumer<ApplicationStatus> listener) {
        return statusChangedEvent.unsubscribe(listener);
    }

    /** Subscribes to newly committed TimingData; no historical replay occurs. */
    public boolean subscribeNewTimingData(Consumer<TimingData> listener) {
        return requireOperationalTimingNode("subscribeNewTimingData")
                .subscribeNewTimingData(listener);
    }

    public boolean unsubscribeNewTimingData(Consumer<TimingData> listener) {
        return requireOperationalTimingNode("unsubscribeNewTimingData")
                .unsubscribeNewTimingData(listener);
    }

    private void publishStatusChangedWhenDifferent(ApplicationStatus before) {
        ApplicationStatus after = status();
        if (!sameStatus(before, after)) {
            statusChangedEvent.emit(after);
        }
    }

    private TimingNode requireOperationalTimingNode(String operation) {
        if (timingNode == null) {
            throw new IllegalStateException(
                    operation + " requires a fully composed TimingNode");
        }
        return timingNode;
    }

    private static boolean sameStatus(
            ApplicationStatus left,
            ApplicationStatus right) {
        if (!left.timingNodeId().equals(right.timingNodeId())) {
            return false;
        }
        if (left.timingNodeLifecycle() != right.timingNodeLifecycle()) {
            return false;
        }
        LocationId leftLocation = left.locationId();
        LocationId rightLocation = right.locationId();
        return leftLocation == null
                ? rightLocation == null
                : leftLocation.equals(rightLocation);
    }

    private static Supplier<ApplicationStatus> statusSupplier(TimingNode timingNode) {
        TimingNode node = requireTimingNode(timingNode);
        return () -> {
            TimingNode.Status status = node.query(TimingNodeQueries.status());
            return new ApplicationStatus(
                    status.timingNodeId(),
                    status.lifecycle(),
                    status.locationId());
        };
    }

    private static TimingNode requireTimingNode(TimingNode timingNode) {
        if (timingNode == null) {
            throw new IllegalArgumentException("timingNode must not be null");
        }
        return timingNode;
    }
}

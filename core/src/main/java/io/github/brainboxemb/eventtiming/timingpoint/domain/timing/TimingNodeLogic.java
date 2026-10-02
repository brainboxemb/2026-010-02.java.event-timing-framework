package io.github.brainboxemb.eventtiming.timingpoint.domain.timing;

import io.github.brainboxemb.eventtiming.timingdata.LocationId;
import io.github.brainboxemb.eventtiming.timingdata.RegistrationId;
import io.github.brainboxemb.eventtiming.timingdata.TimingData;
import io.github.brainboxemb.eventtiming.timingdata.TimingData.ManualTimeSource;
import io.github.brainboxemb.eventtiming.timingdata.TimingDataFactory;
import io.github.brainboxemb.eventtiming.timingdata.TimingDataFactory.Context;
import io.github.brainboxemb.eventtiming.timingdata.TimingNodeId;
import io.github.brainboxemb.eventtiming.timingdata.TimingTimestamp;
import io.github.brainboxemb.eventtiming.timingpoint.domain.logbook.LogBook;
import io.github.brainboxemb.eventtiming.timingpoint.domain.system.TimeSource;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timingdata.TimingDataPersistence;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeTypes.CloseResult;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeTypes.Lifecycle;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeTypes.OpenResult;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeTypes.RegistrationResult;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeTypes.SetLocationResult;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeTypes.Status;

import java.util.List;

/**
 * Internal mutable logic of one TimingNode.
 *
 * <p>This object contains the node state and domain decisions. It has no queue,
 * timeout or event-delivery responsibility; those belong to the visible
 * {@link TimingNode} component boundary.</p>
 */
final class TimingNodeLogic {
    private final TimingNodeId timingNodeId;
    private final LogBook logBook;
    private final TimingDataPersistence timingDataPersistence;
    private final TimingDataFactory timingDataFactory;
    private final TimeSource timeSource;

    private Lifecycle lifecycle = Lifecycle.CLOSED;
    private LocationId locationId;
    private boolean timingDataTailRecovered;
    private Throwable timingDataCommitFailure;

    TimingNodeLogic(TimingNodeId timingNodeId) {
        this(timingNodeId, null, null, null);
    }

    TimingNodeLogic(
            TimingNodeId timingNodeId,
            TimingDataPersistence timingDataPersistence,
            TimingDataFactory timingDataFactory,
            TimeSource timeSource) {
        if (timingNodeId == null) {
            throw new IllegalArgumentException("timingNodeId must not be null");
        }

        boolean hasTimingDataSupport =
                timingDataPersistence != null || timingDataFactory != null || timeSource != null;
        boolean hasCompleteTimingDataSupport =
                timingDataPersistence != null && timingDataFactory != null && timeSource != null;
        if (hasTimingDataSupport && !hasCompleteTimingDataSupport) {
            throw new IllegalArgumentException(
                    "timingDataPersistence, timingDataFactory and timeSource must be configured together");
        }

        this.timingNodeId = timingNodeId;
        this.timingDataPersistence = timingDataPersistence;
        this.timingDataFactory = timingDataFactory;
        this.timeSource = timeSource;
        this.logBook = hasCompleteTimingDataSupport ? new LogBook(timingNodeId) : null;
    }

    TimingNodeId timingNodeId() {
        return timingNodeId;
    }

    boolean hasTimingDataSupport() {
        return logBook != null;
    }

    OpenResult open() {
        if (lifecycle == Lifecycle.OPEN) {
            return OpenResult.ALREADY_OPEN;
        }
        if (locationId == null) {
            return OpenResult.NO_LOCATION;
        }
        lifecycle = Lifecycle.OPEN;
        return OpenResult.OPENED;
    }

    CloseResult close() {
        if (lifecycle == Lifecycle.CLOSED) {
            return CloseResult.ALREADY_CLOSED;
        }
        lifecycle = Lifecycle.CLOSED;
        return CloseResult.CLOSED;
    }

    SetLocationResult setLocation(LocationId newLocationId) {
        if (lifecycle != Lifecycle.CLOSED) {
            return SetLocationResult.NODE_NOT_CLOSED;
        }
        locationId = newLocationId;
        return SetLocationResult.UPDATED;
    }

    RegistrationResult commitAutomaticRegistration(
            RegistrationId registrationId,
            TimingTimestamp observationTime)
            throws TimingDataPersistence.PersistenceException {
        if (lifecycle != Lifecycle.OPEN) {
            return RegistrationResult.nodeNotOpen();
        }
        ensureTimingDataCommitAvailable();

        TimingData data = timingDataFactory.createAutomaticRegistration(
                nextRegistrationContext(observationTime),
                registrationId);
        return commitRegistration(data);
    }

    RegistrationResult commitManualRegistration(
            RegistrationId registrationId,
            TimingTimestamp effectiveTime,
            ManualTimeSource registrationTimeSource)
            throws TimingDataPersistence.PersistenceException {
        if (lifecycle != Lifecycle.OPEN) {
            return RegistrationResult.nodeNotOpen();
        }
        ensureTimingDataCommitAvailable();

        TimingData data = timingDataFactory.createManualRegistration(
                nextRegistrationContext(effectiveTime),
                registrationId,
                registrationTimeSource);
        return commitRegistration(data);
    }

    List<TimingData> timingDataSnapshot() {
        return logBook.snapshot();
    }

    int timingDataCount() {
        return logBook.size();
    }

    List<TimingData> timingDataRange(long fromSequence, int limit) {
        return logBook.range(fromSequence, limit);
    }

    List<TimingData> latestTimingData(int limit) {
        return logBook.latest(limit);
    }

    Status status() {
        return new Status(
                timingNodeId,
                lifecycle,
                locationId,
                timingDataTailRecovered);
    }

    void recoverTimingData() throws TimingDataPersistence.PersistenceException {
        if (logBook == null) {
            return;
        }

        TimingDataPersistence.LoadResult loadResult = timingDataPersistence.load();
        for (TimingData data : loadResult.records()) {
            logBook.add(data);
        }
        timingDataTailRecovered = loadResult.repairedIncompleteTail();
    }

    private Context nextRegistrationContext(TimingTimestamp effectiveTime) {
        return new Context(
                timingNodeId,
                logBook.nextSequence(),
                locationId,
                effectiveTime,
                timeSource.now());
    }

    private void ensureTimingDataCommitAvailable() {
        if (timingDataCommitFailure != null) {
            throw new IllegalStateException(
                    "TimingData commit is blocked after an earlier persistence failure",
                    timingDataCommitFailure);
        }
    }

    private RegistrationResult commitRegistration(TimingData data)
            throws TimingDataPersistence.PersistenceException {
        if (data == null) {
            throw new IllegalStateException("timingDataFactory returned null");
        }

        try {
            timingDataPersistence.append(data);
        } catch (TimingDataPersistence.PersistenceException ex) {
            timingDataCommitFailure = ex;
            throw ex;
        }

        try {
            logBook.add(data);
        } catch (RuntimeException ex) {
            timingDataCommitFailure = ex;
            throw ex;
        }

        return RegistrationResult.committed(data);
    }
}

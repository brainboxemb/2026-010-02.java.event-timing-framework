package io.github.brainboxemb.eventtiming.timingpoint.domain.timing;

import io.github.brainboxemb.eventtiming.timingdata.TimingData.ManualTimeSource;
import io.github.brainboxemb.eventtiming.timingdata.LocationId;
import io.github.brainboxemb.eventtiming.timingdata.RegistrationId;
import io.github.brainboxemb.eventtiming.timingdata.TimingData;
import io.github.brainboxemb.eventtiming.timingdata.TimingDataFactory.Context;
import io.github.brainboxemb.eventtiming.timingdata.TimingDataFactory;
import io.github.brainboxemb.eventtiming.timingdata.TimingTimestamp;
import io.github.brainboxemb.eventtiming.timingpoint.domain.logbook.LogBook;
import io.github.brainboxemb.eventtiming.timingpoint.domain.system.TimeSource;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timingdata.TimingDataStore;
import io.github.brainboxemb.eventtiming.timingpoint.platform.execution.SerialWorker;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Logical timing unit that owns its mutable state behind one serial execution lane.
 *
 * <p>When TimingData support is configured, {@link #start()} first restores the
 * committed TimingData stream into the passive LogBook. Only after successful
 * recovery is the serial worker started and operational work accepted.</p>
 */
public final class TimingNode {
    private static final int DEFAULT_QUEUE_CAPACITY = 32;
    private static final long DEFAULT_OPERATION_TIMEOUT_MILLIS = 2000L;

    public enum Lifecycle {
        CLOSED,
        OPEN
    }

    public enum OpenResult {
        OPENED,
        ALREADY_OPEN,
        NO_LOCATION
    }

    public enum CloseResult {
        CLOSED,
        ALREADY_CLOSED
    }

    public enum SetLocationResult {
        UPDATED,
        NODE_NOT_CLOSED
    }

    public static final class RegistrationResult {
        public enum Outcome {
            COMMITTED,
            NODE_NOT_OPEN
        }

        private final Outcome outcome;
        private final TimingData timingData;

        private RegistrationResult(Outcome outcome, TimingData timingData) {
            this.outcome = outcome;
            this.timingData = timingData;
        }

        private static RegistrationResult committed(TimingData timingData) {
            return new RegistrationResult(Outcome.COMMITTED, timingData);
        }

        private static RegistrationResult nodeNotOpen() {
            return new RegistrationResult(Outcome.NODE_NOT_OPEN, null);
        }

        public Outcome outcome() {
            return outcome;
        }

        public boolean committed() {
            return outcome == Outcome.COMMITTED;
        }

        public TimingData timingData() {
            if (timingData == null) {
                throw new IllegalStateException("registration did not commit TimingData");
            }
            return timingData;
        }
    }

    public static final class Status {
        private final TimingNodeId timingNodeId;
        private final Lifecycle lifecycle;
        private final LocationId locationId;
        private final boolean timingDataTailRecovered;

        private Status(
                TimingNodeId timingNodeId,
                Lifecycle lifecycle,
                LocationId locationId,
                boolean timingDataTailRecovered) {
            this.timingNodeId = timingNodeId;
            this.lifecycle = lifecycle;
            this.locationId = locationId;
            this.timingDataTailRecovered = timingDataTailRecovered;
        }

        public TimingNodeId timingNodeId() {
            return timingNodeId;
        }

        public Lifecycle lifecycle() {
            return lifecycle;
        }

        public boolean hasLocation() {
            return locationId != null;
        }

        public LocationId locationId() {
            return locationId;
        }

        /**
         * Returns whether startup repaired one incomplete unterminated TimingData tail.
         *
         * <p>This is a recovery diagnostic only; it does not mean a complete
         * committed record was discarded.</p>
         */
        public boolean timingDataTailRecovered() {
            return timingDataTailRecovered;
        }
    }

    /** Startup failure before the TimingNode begins accepting serial work. */
    public static final class StartupException extends RuntimeException {
        private StartupException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class OperationException extends RuntimeException {
        public enum Reason {
            BUSY,
            UNAVAILABLE,
            FAILED,
            INTERRUPTED,
            TIMEOUT
        }

        private final Reason reason;

        private OperationException(Reason reason, String message) {
            super(message);
            this.reason = reason;
        }

        private OperationException(Reason reason, String message, Throwable cause) {
            super(message, cause);
            this.reason = reason;
        }

        public Reason reason() {
            return reason;
        }
    }

    /**
     * The caller stopped waiting for an accepted operation.
     *
     * <p>The final domain outcome is unknown to that caller. Accepted work is not
     * cancelled automatically.</p>
     */
    public static final class OperationTimeoutException extends OperationException {
        private OperationTimeoutException(String message, Throwable cause) {
            super(Reason.TIMEOUT, message, cause);
        }
    }

    private final TimingNodeId timingNodeId;
    private final SerialWorker serialWorker;
    private final long operationTimeoutMillis;
    private final LogBook logBook;
    private final TimingDataStore timingDataStore;
    private final TimingDataFactory timingDataFactory;
    private final TimeSource timeSource;

    private Lifecycle lifecycle = Lifecycle.CLOSED;
    private LocationId locationId;
    private boolean timingDataTailRecovered;
    private Throwable startupFailure;
    private Throwable timingDataCommitFailure;

    public TimingNode(TimingNodeId timingNodeId) {
        this(
                timingNodeId,
                new SerialWorker(
                        DEFAULT_QUEUE_CAPACITY,
                        "timing-node-" + requireId(timingNodeId).value()),
                DEFAULT_OPERATION_TIMEOUT_MILLIS,
                null,
                null,
                null);
    }

    public TimingNode(
            TimingNodeId timingNodeId,
            TimingDataStore timingDataStore,
            TimingDataFactory timingDataFactory,
            TimeSource timeSource) {
        this(
                timingNodeId,
                new SerialWorker(
                        DEFAULT_QUEUE_CAPACITY,
                        "timing-node-" + requireId(timingNodeId).value()),
                DEFAULT_OPERATION_TIMEOUT_MILLIS,
                timingDataStore,
                timingDataFactory,
                timeSource);
    }

    TimingNode(
            TimingNodeId timingNodeId,
            SerialWorker serialWorker,
            long operationTimeoutMillis) {
        this(
                timingNodeId,
                serialWorker,
                operationTimeoutMillis,
                null,
                null,
                null);
    }

    TimingNode(
            TimingNodeId timingNodeId,
            SerialWorker serialWorker,
            long operationTimeoutMillis,
            TimingDataStore timingDataStore,
            TimingDataFactory timingDataFactory,
            TimeSource timeSource) {
        this.timingNodeId = requireId(timingNodeId);
        if (serialWorker == null) {
            throw new IllegalArgumentException("serialWorker must not be null");
        }
        if (operationTimeoutMillis < 1L) {
            throw new IllegalArgumentException("operationTimeoutMillis must be positive");
        }

        boolean hasTimingDataSupport =
                timingDataStore != null || timingDataFactory != null || timeSource != null;
        boolean hasCompleteTimingDataSupport =
                timingDataStore != null && timingDataFactory != null && timeSource != null;
        if (hasTimingDataSupport && !hasCompleteTimingDataSupport) {
            throw new IllegalArgumentException(
                    "timingDataStore, timingDataFactory and timeSource must be configured together");
        }

        this.serialWorker = serialWorker;
        this.operationTimeoutMillis = operationTimeoutMillis;
        this.timingDataStore = timingDataStore;
        this.timingDataFactory = timingDataFactory;
        this.timeSource = timeSource;
        this.logBook = hasCompleteTimingDataSupport ? new LogBook(this.timingNodeId) : null;
    }

    private static TimingNodeId requireId(TimingNodeId timingNodeId) {
        if (timingNodeId == null) {
            throw new IllegalArgumentException("timingNodeId must not be null");
        }
        return timingNodeId;
    }

    public TimingNodeId timingNodeId() {
        return timingNodeId;
    }

    /**
     * Restores committed TimingData, when configured, and then starts the serial worker.
     *
     * <p>Recovery never reopens the node and never restores an operational
     * LocationId from historical TimingData. The node starts CLOSED and location
     * selection remains an explicit current-session operation.</p>
     *
     * @throws StartupException when TimingData recovery/rebuild fails
     */
    public void start() {
        if (serialWorker.state() != SerialWorker.State.NEW) {
            throw new IllegalStateException(
                    "TimingNode can only start once; worker state=" + serialWorker.state());
        }
        if (startupFailure != null) {
            throw new StartupException(
                    "TimingNode cannot restart after failed TimingData recovery",
                    startupFailure);
        }

        recoverTimingData();
        serialWorker.start();
    }

    public void stop() {
        serialWorker.close();
    }

    public OpenResult open() {
        return execute(this::doOpen, "open");
    }

    public CloseResult close() {
        return execute(this::doClose, "close");
    }

    public SetLocationResult setLocation(LocationId newLocationId) {
        if (newLocationId == null) {
            throw new IllegalArgumentException("newLocationId must not be null");
        }
        return execute(() -> doSetLocation(newLocationId), "setLocation");
    }

    public RegistrationResult registerManual(
            RegistrationId registrationId,
            TimingTimestamp effectiveTime,
            ManualTimeSource registrationTimeSource) {
        if (registrationId == null) {
            throw new IllegalArgumentException("registrationId must not be null");
        }
        if (effectiveTime == null) {
            throw new IllegalArgumentException("effectiveTime must not be null");
        }
        if (registrationTimeSource == null) {
            throw new IllegalArgumentException("registrationTimeSource must not be null");
        }
        requireTimingDataSupport("registerManual");
        return execute(
                () -> doRegisterManual(
                        registrationId,
                        effectiveTime,
                        registrationTimeSource),
                "registerManual");
    }

    public List<TimingData> timingDataSnapshot() {
        requireTimingDataSupport("timingDataSnapshot");
        return execute(logBook::snapshot, "timingDataSnapshot");
    }

    public Status status() {
        return execute(this::snapshotStatus, "status");
    }

    private OpenResult doOpen() {
        if (lifecycle == Lifecycle.OPEN) {
            return OpenResult.ALREADY_OPEN;
        }
        if (locationId == null) {
            return OpenResult.NO_LOCATION;
        }
        lifecycle = Lifecycle.OPEN;
        return OpenResult.OPENED;
    }

    private CloseResult doClose() {
        if (lifecycle == Lifecycle.CLOSED) {
            return CloseResult.ALREADY_CLOSED;
        }
        lifecycle = Lifecycle.CLOSED;
        return CloseResult.CLOSED;
    }

    private SetLocationResult doSetLocation(LocationId newLocationId) {
        if (lifecycle != Lifecycle.CLOSED) {
            return SetLocationResult.NODE_NOT_CLOSED;
        }
        locationId = newLocationId;
        return SetLocationResult.UPDATED;
    }

    private RegistrationResult doRegisterManual(
            RegistrationId registrationId,
            TimingTimestamp effectiveTime,
            ManualTimeSource registrationTimeSource)
            throws TimingDataStore.StoreException {
        if (lifecycle != Lifecycle.OPEN) {
            return RegistrationResult.nodeNotOpen();
        }
        if (timingDataCommitFailure != null) {
            throw new IllegalStateException(
                    "TimingData commit is blocked after an earlier persistence failure",
                    timingDataCommitFailure);
        }

        long sequence = logBook.nextSequence();
        Context context = new Context(
                timingNodeId.value(),
                sequence,
                locationId,
                effectiveTime,
                timeSource.now());
        TimingData data = timingDataFactory.createManualRegistration(
                context,
                registrationId,
                registrationTimeSource);
        if (data == null) {
            throw new IllegalStateException("timingDataFactory returned null");
        }

        try {
            timingDataStore.append(data);
        } catch (TimingDataStore.StoreException ex) {
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

    private void recoverTimingData() {
        if (logBook == null) {
            return;
        }

        try {
            TimingDataStore.LoadResult loadResult = timingDataStore.load();
            for (TimingData data : loadResult.records()) {
                logBook.add(data);
            }
            timingDataTailRecovered = loadResult.repairedIncompleteTail();
        } catch (TimingDataStore.StoreException | RuntimeException ex) {
            startupFailure = ex;
            throw new StartupException(
                    "TimingData recovery failed for " + timingNodeId.value(),
                    ex);
        }
    }

    private Status snapshotStatus() {
        return new Status(
                timingNodeId,
                lifecycle,
                locationId,
                timingDataTailRecovered);
    }

    private void requireTimingDataSupport(String operation) {
        if (logBook == null) {
            throw new OperationException(
                    OperationException.Reason.UNAVAILABLE,
                    operation + " is unavailable because TimingData recording is not configured");
        }
    }

    private <R> R execute(Callable<R> work, String operation) {
        SerialWorker.SubmitResult<R> submitResult = serialWorker.submit(work);
        switch (submitResult.admission()) {
            case FULL:
                throw new OperationException(
                        OperationException.Reason.BUSY,
                        operation + " could not be admitted because the TimingNode queue is full");
            case NOT_RUNNING:
                if (serialWorker.state() == SerialWorker.State.FAILED) {
                    throw new OperationException(
                            OperationException.Reason.FAILED,
                            operation + " could not run because the TimingNode worker failed",
                            serialWorker.failure());
                }
                throw new OperationException(
                        OperationException.Reason.UNAVAILABLE,
                        operation + " could not be admitted because the TimingNode is not running");
            case ACCEPTED:
                return await(submitResult.futureResult(), operation);
            default:
                throw new IllegalStateException(
                        "Unsupported submission result " + submitResult.admission());
        }
    }

    private <R> R await(Future<R> future, String operation) {
        try {
            return future.get(operationTimeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            throw new OperationTimeoutException(
                    operation + " timed out; final TimingNode outcome is unknown",
                    ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new OperationException(
                    OperationException.Reason.INTERRUPTED,
                    operation + " was interrupted while waiting for the TimingNode result",
                    ex);
        } catch (CancellationException ex) {
            throw new OperationException(
                    OperationException.Reason.FAILED,
                    operation + " was cancelled because the TimingNode worker failed",
                    ex);
        } catch (ExecutionException ex) {
            throw new OperationException(
                    OperationException.Reason.FAILED,
                    operation + " failed while executing on the TimingNode",
                    ex.getCause());
        }
    }
}

package io.github.brainboxemb.eventtiming.timingpoint.domain.timing;

import io.github.brainboxemb.eventtiming.timingdata.LocationId;
import io.github.brainboxemb.eventtiming.timingdata.RegistrationId;
import io.github.brainboxemb.eventtiming.timingdata.TimingData;
import io.github.brainboxemb.eventtiming.timingdata.TimingData.ManualTimeSource;
import io.github.brainboxemb.eventtiming.timingdata.TimingDataFactory;
import io.github.brainboxemb.eventtiming.timingdata.TimingNodeId;
import io.github.brainboxemb.eventtiming.timingdata.TimingTimestamp;
import io.github.brainboxemb.eventtiming.timingpoint.domain.system.TimeSource;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timingdata.TimingDataStore;
import io.github.brainboxemb.eventtiming.timingpoint.platform.events.Event;
import io.github.brainboxemb.eventtiming.timingpoint.platform.execution.SerialWorker;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Public component boundary for one logical timing node.
 *
 * <p>The node serializes external operations and delegates its mutable domain
 * behaviour to {@link TimingNodeLogic}. Higher layers use this component rather
 * than the internal logic object directly.</p>
 */
public final class TimingNode {
    private static final Logger LOG = LoggerFactory.getLogger(TimingNode.class);
    private static final int DEFAULT_QUEUE_CAPACITY = 32;
    private static final long DEFAULT_OPERATION_TIMEOUT_MILLIS = 2000L;

    private final TimingNodeLogic logic;
    private final SerialWorker serialWorker;
    private final long operationTimeoutMillis;
    private final Event<TimingData> newTimingDataEvent = new Event<>();

    private Throwable startupFailure;

    public TimingNode(TimingNodeId timingNodeId) {
        this(
                new TimingNodeLogic(timingNodeId),
                workerFor(timingNodeId),
                DEFAULT_OPERATION_TIMEOUT_MILLIS);
    }

    public TimingNode(
            TimingNodeId timingNodeId,
            TimingDataStore timingDataStore,
            TimingDataFactory timingDataFactory,
            TimeSource timeSource) {
        this(
                new TimingNodeLogic(
                        timingNodeId,
                        timingDataStore,
                        timingDataFactory,
                        timeSource),
                workerFor(timingNodeId),
                DEFAULT_OPERATION_TIMEOUT_MILLIS);
    }

    TimingNode(
            TimingNodeId timingNodeId,
            SerialWorker serialWorker,
            long operationTimeoutMillis) {
        this(
                new TimingNodeLogic(timingNodeId),
                serialWorker,
                operationTimeoutMillis);
    }

    TimingNode(
            TimingNodeId timingNodeId,
            SerialWorker serialWorker,
            long operationTimeoutMillis,
            TimingDataStore timingDataStore,
            TimingDataFactory timingDataFactory,
            TimeSource timeSource) {
        this(
                new TimingNodeLogic(
                        timingNodeId,
                        timingDataStore,
                        timingDataFactory,
                        timeSource),
                serialWorker,
                operationTimeoutMillis);
    }

    private TimingNode(
            TimingNodeLogic logic,
            SerialWorker serialWorker,
            long operationTimeoutMillis) {
        if (logic == null) {
            throw new IllegalArgumentException("logic must not be null");
        }
        if (serialWorker == null) {
            throw new IllegalArgumentException("serialWorker must not be null");
        }
        if (operationTimeoutMillis < 1L) {
            throw new IllegalArgumentException("operationTimeoutMillis must be positive");
        }
        this.logic = logic;
        this.serialWorker = serialWorker;
        this.operationTimeoutMillis = operationTimeoutMillis;
    }

    public TimingNodeId timingNodeId() {
        return logic.timingNodeId();
    }

    /**
     * Recovers committed TimingData before accepting serial operations.
     *
     * <p>Recovery does not restore the operational LocationId or OPEN state.</p>
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

        try {
            logic.recoverTimingData();
        } catch (TimingDataStore.StoreException | RuntimeException ex) {
            startupFailure = ex;
            throw new StartupException(
                    "TimingData recovery failed for " + timingNodeId().value(),
                    ex);
        }
        serialWorker.start();
    }

    public void stop() {
        serialWorker.close();
    }

    public OpenResult open() {
        return execute(logic::open, "open");
    }

    public CloseResult close() {
        return execute(logic::close, "close");
    }

    public SetLocationResult setLocation(LocationId locationId) {
        if (locationId == null) {
            throw new IllegalArgumentException("locationId must not be null");
        }
        return execute(() -> logic.setLocation(locationId), "setLocation");
    }

    /**
     * Commits one automatic registration that already passed source-specific
     * interpretation and filtering.
     */
    public RegistrationResult commitAutomaticRegistration(
            RegistrationId registrationId,
            TimingTimestamp observationTime) {
        if (registrationId == null) {
            throw new IllegalArgumentException("registrationId must not be null");
        }
        if (observationTime == null) {
            throw new IllegalArgumentException("observationTime must not be null");
        }
        requireTimingDataSupport("commitAutomaticRegistration");
        return execute(
                () -> publishCommitted(
                        logic.commitAutomaticRegistration(
                                registrationId,
                                observationTime)),
                "commitAutomaticRegistration");
    }

    public RegistrationResult commitManualRegistration(
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
        requireTimingDataSupport("commitManualRegistration");
        return execute(
                () -> publishCommitted(
                        logic.commitManualRegistration(
                                registrationId,
                                effectiveTime,
                                registrationTimeSource)),
                "commitManualRegistration");
    }

    public boolean subscribeNewTimingData(Consumer<TimingData> listener) {
        requireTimingDataSupport("subscribeNewTimingData");
        return newTimingDataEvent.subscribe(listener);
    }

    public boolean unsubscribeNewTimingData(Consumer<TimingData> listener) {
        requireTimingDataSupport("unsubscribeNewTimingData");
        return newTimingDataEvent.unsubscribe(listener);
    }

    /**
     * Executes a typed read against the same serial lane as state-changing commands.
     *
     * <p>Queries carry the read operation instead of requiring a forwarding method
     * on TimingNode for every value exposed by TimingNodeLogic. This keeps reads
     * ordered with commands while the visible TimingNode API stays compact.</p>
     */
    public <R> R query(TimingNodeQuery<R> query) {
        if (query == null) {
            throw new IllegalArgumentException("query must not be null");
        }
        if (query.requiresTimingData()) {
            requireTimingDataSupport(query.name());
        }
        return execute(() -> query.read(logic), query.name());
    }

    private RegistrationResult publishCommitted(RegistrationResult result) {
        if (!result.committed()) {
            return result;
        }

        TimingData data = result.timingData();
        Event.DeliveryReport delivery = newTimingDataEvent.emit(data);
        if (!delivery.successful()) {
            LOG.warn(
                    "TimingData committed but "
                            + delivery.failureCount()
                            + " newTimingData listener(s) failed for "
                            + timingNodeId().value()
                            + ":"
                            + data.sequenceNumber(),
                    delivery.failures().get(0));
        }
        return result;
    }

    private void requireTimingDataSupport(String operation) {
        if (!logic.hasTimingDataSupport()) {
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

    private static SerialWorker workerFor(TimingNodeId timingNodeId) {
        TimingNodeId id = requireId(timingNodeId);
        return new SerialWorker(
                DEFAULT_QUEUE_CAPACITY,
                "timing-node-" + id.value());
    }

    private static TimingNodeId requireId(TimingNodeId timingNodeId) {
        if (timingNodeId == null) {
            throw new IllegalArgumentException("timingNodeId must not be null");
        }
        return timingNodeId;
    }

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

        static RegistrationResult committed(TimingData timingData) {
            return new RegistrationResult(Outcome.COMMITTED, timingData);
        }

        static RegistrationResult nodeNotOpen() {
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

        Status(
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

        public boolean timingDataTailRecovered() {
            return timingDataTailRecovered;
        }
    }

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

    public static final class OperationTimeoutException extends OperationException {
        private OperationTimeoutException(String message, Throwable cause) {
            super(Reason.TIMEOUT, message, cause);
        }
    }
}

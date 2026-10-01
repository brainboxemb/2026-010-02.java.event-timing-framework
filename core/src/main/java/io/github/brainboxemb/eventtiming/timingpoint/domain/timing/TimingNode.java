package io.github.brainboxemb.eventtiming.timingpoint.domain.timing;

import io.github.brainboxemb.eventtiming.timingpoint.platform.execution.SerialWorker;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Logical timing unit that owns its mutable state behind one serial execution lane. */
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

    public static final class Status {
        private final TimingNodeId timingNodeId;
        private final Lifecycle lifecycle;
        private final LocationId locationId;

        private Status(
                TimingNodeId timingNodeId,
                Lifecycle lifecycle,
                LocationId locationId) {
            this.timingNodeId = timingNodeId;
            this.lifecycle = lifecycle;
            this.locationId = locationId;
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

    private Lifecycle lifecycle = Lifecycle.CLOSED;
    private LocationId locationId;

    public TimingNode(TimingNodeId timingNodeId) {
        this(
                timingNodeId,
                new SerialWorker(
                        DEFAULT_QUEUE_CAPACITY,
                        "timing-node-" + requireId(timingNodeId).value()),
                DEFAULT_OPERATION_TIMEOUT_MILLIS);
    }

    TimingNode(
            TimingNodeId timingNodeId,
            SerialWorker serialWorker,
            long operationTimeoutMillis) {
        this.timingNodeId = requireId(timingNodeId);
        if (serialWorker == null) {
            throw new IllegalArgumentException("serialWorker must not be null");
        }
        if (operationTimeoutMillis < 1L) {
            throw new IllegalArgumentException("operationTimeoutMillis must be positive");
        }
        this.serialWorker = serialWorker;
        this.operationTimeoutMillis = operationTimeoutMillis;
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

    public void start() {
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

    private Status snapshotStatus() {
        return new Status(timingNodeId, lifecycle, locationId);
    }

    private <R> R execute(Callable<R> work, String operation) {
        SerialWorker.Submission<R> submission = serialWorker.submit(work);
        switch (submission.result()) {
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
                return await(submission.future(), operation);
            default:
                throw new IllegalStateException(
                        "Unsupported submission result " + submission.result());
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

package io.github.brainboxemb.eventtiming.timingpoint.platform.execution;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * Executes accepted work items one at a time, in FIFO order, on one dedicated thread.
 *
 * <p>This is a small platform primitive. It deliberately knows nothing about
 * TimingNode, persistence or domain validation. Its responsibility is limited to:
 *
 * <ul>
 *   <li>bounded queue admission;</li>
 *   <li>serial execution of accepted work;</li>
 *   <li>a {@link Future} for work that has a processed result;</li>
 *   <li>observable lifecycle, overload and worker failure.</li>
 * </ul>
 *
 * <p><strong>Queue admission and operation result are different things.</strong>
 * A successful {@link AdmissionResult#ACCEPTED} only means that the work item was
 * accepted into this serial execution lane. It does not mean that the work item
 * has executed successfully or that a domain operation was accepted.</p>
 *
 * <p>Typical result-bearing usage:</p>
 *
 * <pre>{@code
 * SerialWorker worker = new SerialWorker(32, "timing-node-01");
 * worker.start();
 *
 * SerialWorker.SubmitResult<OpenResult> submitResult =
 *         worker.submit(this::doOpen);
 *
 * if (submitResult.admission() == SerialWorker.AdmissionResult.ACCEPTED) {
 *     Future<OpenResult> futureResult = submitResult.futureResult();
 *     OpenResult processedResult = futureResult.get();
 * }
 *
 * worker.close();
 * }</pre>
 *
 * <p>Use {@link #offer(Runnable)} when a producer needs to know only whether work
 * was admitted and intentionally does not wait for a processed result.</p>
 *
 * <p>{@link #close()} stops new admission and drains work that was already
 * accepted. An ordinary work-item exception is reported through its Future and
 * does not stop the worker. A fatal {@link Error} faults the worker and cancels
 * work that was still queued.</p>
 */
public final class SerialWorker implements AutoCloseable {

    /** Runtime state of the dedicated serial worker thread. */
    public enum State {
        /** Constructed but not started. No work is accepted. */
        NEW,
        /** Accepting and processing work. */
        RUNNING,
        /** No longer accepting new work; previously accepted work is draining. */
        STOPPING,
        /** Draining completed and the worker thread stopped normally. */
        STOPPED,
        /** The worker stopped after a fatal failure. */
        FAILED
    }

    /**
     * Immediate queue-admission result.
     *
     * <p>This is deliberately not the result of the submitted operation itself.</p>
     */
    public enum AdmissionResult {
        /** The work item was accepted for later serial execution. */
        ACCEPTED,
        /** The bounded queue had no remaining capacity. The work was not accepted. */
        FULL,
        /** The worker is not in RUNNING state. The work was not accepted. */
        NOT_RUNNING
    }

    /**
     * Result of attempting to submit one result-bearing work item.
     *
     * <p>{@link #admission()} is available for every submission attempt.
     * {@link #futureResult()} is available only when admission was
     * {@link AdmissionResult#ACCEPTED}. The Future then represents the later
     * processed result of the Callable, not queue admission.</p>
     *
     * @param <R> processed result type returned by the submitted Callable
     */
    public static final class SubmitResult<R> {
        private final AdmissionResult admission;
        private final Future<R> futureResult;

        private SubmitResult(AdmissionResult admission, Future<R> futureResult) {
            this.admission = admission;
            this.futureResult = futureResult;
        }

        /** Returns the immediate bounded-queue admission result. */
        public AdmissionResult admission() {
            return admission;
        }

        /**
         * Returns the asynchronous processed result for accepted work.
         *
         * @throws IllegalStateException if the work item was not accepted
         */
        public Future<R> futureResult() {
            if (futureResult == null) {
                throw new IllegalStateException(
                        "futureResult is unavailable because the work was not accepted");
            }
            return futureResult;
        }
    }

    private static final long IDLE_POLL_MILLIS = 50L;

    private final ArrayBlockingQueue<ResultTask<?>> queue;
    private final String threadName;

    private State state = State.NEW;
    private Thread thread;
    private Throwable failure;
    private int highWaterMark;

    /**
     * Creates one stopped serial worker.
     *
     * @param capacity maximum number of queued work items, excluding the item
     *                 currently executing
     * @param threadName name of the dedicated worker thread
     */
    public SerialWorker(int capacity, String threadName) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        if (threadName == null || threadName.trim().isEmpty()) {
            throw new IllegalArgumentException("threadName must not be blank");
        }
        this.queue = new ArrayBlockingQueue<>(capacity);
        this.threadName = threadName.trim();
    }

    /**
     * Starts accepting and processing work.
     *
     * <p>A SerialWorker has one lifecycle and cannot be restarted after stop/failure.</p>
     */
    public synchronized void start() {
        if (state != State.NEW) {
            throw new IllegalStateException(
                    "SerialWorker can only start from NEW; current state=" + state);
        }
        state = State.RUNNING;
        thread = new Thread(this::runLoop, threadName);
        thread.start();
    }

    /**
     * Attempts to admit result-bearing work without blocking for queue space.
     *
     * <p>When admission is ACCEPTED, call {@link SubmitResult#futureResult()} to
     * observe the result produced later when the Callable executes on the serial lane.</p>
     *
     * @param work operation to execute serially
     * @param <R> processed result type
     * @return immediate admission plus, for accepted work, its Future result
     */
    public <R> SubmitResult<R> submit(Callable<R> work) {
        if (work == null) {
            throw new IllegalArgumentException("work must not be null");
        }
        ResultTask<R> task = new ResultTask<>(work);
        AdmissionResult admission = offerTask(task);
        return new SubmitResult<>(
                admission,
                admission == AdmissionResult.ACCEPTED ? task : null);
    }

    /**
     * Attempts to admit submission-only work without blocking for queue space.
     *
     * <p>Use this for producer paths that intentionally need only definite queue
     * admission and do not need a later processed result.</p>
     */
    public AdmissionResult offer(Runnable work) {
        if (work == null) {
            throw new IllegalArgumentException("work must not be null");
        }
        return offerTask(new ResultTask<>(() -> {
            work.run();
            return null;
        }));
    }

    private synchronized AdmissionResult offerTask(ResultTask<?> task) {
        if (state != State.RUNNING) {
            return AdmissionResult.NOT_RUNNING;
        }
        if (!queue.offer(task)) {
            return AdmissionResult.FULL;
        }
        int depth = queue.size();
        if (depth > highWaterMark) {
            highWaterMark = depth;
        }
        return AdmissionResult.ACCEPTED;
    }

    private void runLoop() {
        try {
            while (shouldContinue()) {
                ResultTask<?> task = queue.poll(IDLE_POLL_MILLIS, TimeUnit.MILLISECONDS);
                if (task == null) {
                    continue;
                }
                task.run();
                Error fatal = task.fatalError();
                if (fatal != null) {
                    throw fatal;
                }
            }
            markStopped();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            markFailed(ex);
        } catch (Throwable ex) {
            markFailed(ex);
        }
    }

    private synchronized boolean shouldContinue() {
        return state == State.RUNNING || (state == State.STOPPING && !queue.isEmpty());
    }

    private synchronized void markStopped() {
        if (state != State.FAILED) {
            state = State.STOPPED;
        }
        notifyAll();
    }

    private void markFailed(Throwable cause) {
        synchronized (this) {
            failure = cause;
            state = State.FAILED;
            notifyAll();
        }
        ResultTask<?> queued;
        while ((queued = queue.poll()) != null) {
            queued.cancel(false);
        }
    }

    /** Returns the current worker lifecycle state. */
    public synchronized State state() {
        return state;
    }

    /** Returns the number of work items currently waiting in the bounded queue. */
    public int queueDepth() {
        return queue.size();
    }

    /**
     * Returns the highest queued depth observed since construction.
     *
     * <p>This is intended for engineering/target-capacity measurements.</p>
     */
    public synchronized int highWaterMark() {
        return highWaterMark;
    }

    /**
     * Returns the fatal worker failure, or {@code null} when no fatal worker
     * failure has occurred.
     */
    public synchronized Throwable failure() {
        return failure;
    }

    /**
     * Stops new admission, drains already accepted work and waits for the worker
     * thread to finish.
     *
     * <p>Closing a never-started worker moves it directly to STOPPED. Closing an
     * already stopped or failed worker is a no-op.</p>
     */
    @Override
    public void close() {
        Thread worker;
        synchronized (this) {
            if (state == State.NEW) {
                state = State.STOPPED;
                notifyAll();
                return;
            }
            if (state == State.RUNNING) {
                state = State.STOPPING;
            }
            if (state == State.STOPPED || state == State.FAILED) {
                return;
            }
            worker = thread;
        }

        if (worker == null || worker == Thread.currentThread()) {
            return;
        }

        boolean interrupted = false;
        while (worker.isAlive()) {
            try {
                worker.join();
            } catch (InterruptedException ex) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Internal bridge between queued Runnable execution and the public Future result.
     */
    private static final class ResultTask<R> implements Runnable, Future<R> {
        private final FutureTask<R> delegate;
        private volatile Error fatalError;

        private ResultTask(Callable<R> work) {
            this.delegate = new FutureTask<>(() -> {
                try {
                    return work.call();
                } catch (Error ex) {
                    fatalError = ex;
                    throw ex;
                }
            });
        }

        private Error fatalError() {
            return fatalError;
        }

        @Override
        public void run() {
            delegate.run();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return delegate.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return delegate.isCancelled();
        }

        @Override
        public boolean isDone() {
            return delegate.isDone();
        }

        @Override
        public R get() throws java.lang.InterruptedException,
                java.util.concurrent.ExecutionException {
            return delegate.get();
        }

        @Override
        public R get(long timeout, TimeUnit unit)
                throws java.lang.InterruptedException,
                java.util.concurrent.ExecutionException,
                java.util.concurrent.TimeoutException {
            return delegate.get(timeout, unit);
        }
    }
}

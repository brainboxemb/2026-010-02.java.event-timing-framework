package io.github.brainboxemb.eventtiming.timingpoint.platform.execution;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * Small bounded one-at-a-time execution primitive.
 *
 * <p>The worker has no TimingNode or persistence semantics. It provides a visible
 * bounded FIFO lane for both result-bearing work and submission-only work.</p>
 */
public final class SerialWorker implements AutoCloseable {
    public enum State {
        NEW,
        RUNNING,
        STOPPING,
        STOPPED,
        FAILED
    }

    public enum SubmissionResult {
        ACCEPTED,
        FULL,
        NOT_RUNNING
    }

    public static final class Submission<R> {
        private final SubmissionResult result;
        private final Future<R> future;

        private Submission(SubmissionResult result, Future<R> future) {
            this.result = result;
            this.future = future;
        }

        public SubmissionResult result() {
            return result;
        }

        public Future<R> future() {
            if (future == null) {
                throw new IllegalStateException("submission was not accepted");
            }
            return future;
        }
    }

    private static final long IDLE_POLL_MILLIS = 50L;

    private final ArrayBlockingQueue<ResultTask<?>> queue;
    private final String threadName;

    private State state = State.NEW;
    private Thread thread;
    private Throwable failure;
    private int highWaterMark;

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

    public synchronized void start() {
        if (state != State.NEW) {
            throw new IllegalStateException(
                    "SerialWorker can only start from NEW; current state=" + state);
        }
        state = State.RUNNING;
        thread = new Thread(this::runLoop, threadName);
        thread.start();
    }

    public <R> Submission<R> submit(Callable<R> work) {
        if (work == null) {
            throw new IllegalArgumentException("work must not be null");
        }
        ResultTask<R> task = new ResultTask<>(work);
        SubmissionResult result = offerTask(task);
        return new Submission<>(result, result == SubmissionResult.ACCEPTED ? task : null);
    }

    public SubmissionResult offer(Runnable work) {
        if (work == null) {
            throw new IllegalArgumentException("work must not be null");
        }
        return offerTask(new ResultTask<>(() -> {
            work.run();
            return null;
        }));
    }

    private synchronized SubmissionResult offerTask(ResultTask<?> task) {
        if (state != State.RUNNING) {
            return SubmissionResult.NOT_RUNNING;
        }
        if (!queue.offer(task)) {
            return SubmissionResult.FULL;
        }
        int depth = queue.size();
        if (depth > highWaterMark) {
            highWaterMark = depth;
        }
        return SubmissionResult.ACCEPTED;
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

    public synchronized State state() {
        return state;
    }

    public int queueDepth() {
        return queue.size();
    }

    public synchronized int highWaterMark() {
        return highWaterMark;
    }

    public synchronized Throwable failure() {
        return failure;
    }

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

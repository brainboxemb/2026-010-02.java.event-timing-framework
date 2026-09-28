package io.github.brainboxemb.eventtiming.runtime;

import io.github.brainboxemb.eventtiming.infra.BuildIdentity;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Reusable application runtime lifecycle used by startup and graceful shutdown handling. */
public final class TimingApplicationLifecycle implements AutoCloseable {
    public enum State {
        NEW,
        RUNNING,
        STOPPED
    }

    private static final Logger LOG = LoggerFactory.getLogger(TimingApplicationLifecycle.class);

    private final BuildIdentity buildIdentity;
    private State state = State.NEW;
    private Instant startedAt;

    public TimingApplicationLifecycle(BuildIdentity buildIdentity) {
        if (buildIdentity == null) {
            throw new IllegalArgumentException("buildIdentity must not be null");
        }
        this.buildIdentity = buildIdentity;
    }

    public synchronized void start() {
        if (state != State.NEW) {
            throw new IllegalStateException(
                    "Application can only start from NEW; current state=" + state);
        }
        LOG.info("Starting {} ({})", buildIdentity.displayName(), buildIdentity.provenance());
        startedAt = Instant.now();
        state = State.RUNNING;
        LOG.info("Application lifecycle state={}", state);
    }

    public synchronized void stop() {
        if (state != State.RUNNING) {
            throw new IllegalStateException(
                    "Application can only stop from RUNNING; current state=" + state);
        }
        state = State.STOPPED;
        notifyAll();
        LOG.info("Stopped {} with lifecycle state={}", buildIdentity.displayName(), state);
    }

    public synchronized State state() {
        return state;
    }

    public synchronized Instant startedAt() {
        if (startedAt == null) {
            throw new IllegalStateException("Application has not started");
        }
        return startedAt;
    }

    public synchronized void awaitStopped() throws InterruptedException {
        while (state != State.STOPPED) {
            wait();
        }
    }

    @Override
    public synchronized void close() {
        if (state == State.RUNNING) {
            stop();
        } else if (state == State.NEW) {
            state = State.STOPPED;
            notifyAll();
            LOG.info(
                    "Closed {} before start; lifecycle state={}",
                    buildIdentity.displayName(),
                    state);
        }
    }
}

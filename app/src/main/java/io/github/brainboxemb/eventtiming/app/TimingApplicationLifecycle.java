package io.github.brainboxemb.eventtiming.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Minimal Step-2 lifecycle owned by the executable application composition. */
public final class TimingApplicationLifecycle implements AutoCloseable {
    public enum State {
        NEW,
        RUNNING,
        STOPPED
    }

    private static final Logger LOG = LoggerFactory.getLogger(TimingApplicationLifecycle.class);

    private final BuildIdentity buildIdentity;
    private State state = State.NEW;

    public TimingApplicationLifecycle(BuildIdentity buildIdentity) {
        if (buildIdentity == null) {
            throw new IllegalArgumentException("buildIdentity must not be null");
        }
        this.buildIdentity = buildIdentity;
    }

    public synchronized void start() {
        if (state != State.NEW) {
            throw new IllegalStateException("Application can only start from NEW; current state=" + state);
        }
        LOG.info("Starting {}", buildIdentity.displayName());
        state = State.RUNNING;
        LOG.info("Application lifecycle state={}", state);
    }

    public synchronized void stop() {
        if (state != State.RUNNING) {
            throw new IllegalStateException("Application can only stop from RUNNING; current state=" + state);
        }
        state = State.STOPPED;
        LOG.info("Stopped {} with lifecycle state={}", buildIdentity.displayName(), state);
    }

    public synchronized State state() {
        return state;
    }

    @Override
    public synchronized void close() {
        if (state == State.RUNNING) {
            stop();
        } else if (state == State.NEW) {
            state = State.STOPPED;
            LOG.info("Closed {} before start; lifecycle state={}", buildIdentity.displayName(), state);
        }
    }
}

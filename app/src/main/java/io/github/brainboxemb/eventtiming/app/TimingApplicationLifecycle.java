package io.github.brainboxemb.eventtiming.app;

import io.github.brainboxemb.eventtiming.application.BuildIdentity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minimal executable-application lifecycle for the Step-2 baseline.
 *
 * <p>This lifecycle deliberately belongs to the application composition rather than the reusable
 * framework. Later runtime services can be started/stopped from here without making the framework
 * itself one fixed executable product. See the SI-01 application architecture and
 * {@code docs/31-01-SDD-02-java-component-design.md} in the meta repository.</p>
 */
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

    /** Starts the application exactly once from the {@link State#NEW} state. */
    public synchronized void start() {
        if (state != State.NEW) {
            throw new IllegalStateException("Application can only start from NEW; current state=" + state);
        }
        LOG.info("Starting {} ({})", buildIdentity.displayName(), buildIdentity.provenance());
        state = State.RUNNING;
        LOG.info("Application lifecycle state={}", state);
    }

    /** Stops a running application and records the terminal {@link State#STOPPED} state. */
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

    /**
     * Ensures application-owned runtime resources are stopped when the composition is closed.
     * Closing before {@link #start()} is allowed so startup failure paths can use the same cleanup
     * structure as normal shutdown.
     */
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

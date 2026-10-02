package io.github.brainboxemb.eventtiming.timingpoint.platform.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Small typed local event for post-fact notifications.
 *
 * <p>Event deliberately has no hidden executor, topic router or retry queue.
 * {@link #emit(Object)} invokes the current listeners synchronously, in
 * subscription order, on the emitting thread. A listener that needs slow I/O,
 * retry or network delivery must hand the immutable value to its own bounded
 * execution/delivery mechanism and return quickly.</p>
 *
 * <p>The subscription registry is thread-safe. Subscribe, unsubscribe and emit
 * may be called from different threads without corrupting the listener set or
 * causing concurrent-modification failures. Each emit iterates a stable
 * listener snapshot: a listener added during that emit participates only in a
 * later emit; a listener removed during that emit may still receive the value
 * already being delivered.</p>
 *
 * <p>Event deliberately does <strong>not</strong> serialize concurrent emits.
 * If two threads call {@code emit(...)} at the same time, the same listener may
 * be invoked concurrently on those two emitting threads. An event owner that
 * requires strict ordering/non-overlap must emit from its own serial execution
 * boundary. Listener implementations must therefore be thread-safe whenever
 * their owning event can be emitted concurrently.</p>
 *
 * <p>Ordinary listener {@link RuntimeException}s are isolated per listener:
 * later listeners are still invoked and the failures are returned in a
 * {@link DeliveryReport}. Fatal {@link Error}s are not swallowed.</p>
 *
 * <p>Typical usage:</p>
 *
 * <pre>{@code
 * Event<TimingData> event = new Event<>();
 *
 * event.subscribe(data -> outboundQueue.offer(data));
 *
 * Event.DeliveryReport report = event.emit(committedData);
 * if (!report.successful()) {
 *     // Record/report notification failure; committedData stays committed.
 * }
 * }</pre>
 *
 * @param <T> immutable/read-only value delivered to listeners
 */
public final class Event<T> implements EventSource<T> {
    private final CopyOnWriteArrayList<Consumer<T>> listeners =
            new CopyOnWriteArrayList<>();

    /**
     * Subscribes one listener.
     *
     * <p>The same listener instance is stored at most once. A listener added
     * while an emission is already in progress participates from the next
     * emission onward.</p>
     *
     * @return {@code true} when the listener was newly added
     */
    @Override
    public boolean subscribe(Consumer<T> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        return listeners.addIfAbsent(listener);
    }

    /**
     * Removes one listener when present.
     *
     * @return {@code true} when a subscription was removed
     */
    @Override
    public boolean unsubscribe(Consumer<T> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        return listeners.remove(listener);
    }

    /**
     * Delivers one value to the listener snapshot that exists at emit start.
     *
     * <p>This method does not throw ordinary listener RuntimeExceptions. They are
     * collected into the returned report so one listener cannot prevent delivery
     * to later listeners. Fatal Errors still propagate to the owner thread.</p>
     */
    public DeliveryReport emit(T value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }

        int attempted = 0;
        List<RuntimeException> failures = null;

        for (Consumer<T> listener : listeners) {
            attempted++;
            try {
                listener.accept(value);
            } catch (RuntimeException ex) {
                if (failures == null) {
                    failures = new ArrayList<>();
                }
                failures.add(ex);
            }
        }

        return new DeliveryReport(
                attempted,
                failures == null ? Collections.emptyList() : failures);
    }

    /** Immutable outcome of one synchronous emission. */
    public static final class DeliveryReport {
        private final int attemptedListeners;
        private final List<RuntimeException> failures;

        private DeliveryReport(
                int attemptedListeners,
                List<RuntimeException> failures) {
            this.attemptedListeners = attemptedListeners;
            this.failures = Collections.unmodifiableList(
                    new ArrayList<>(failures));
        }

        /** Number of listeners that were invoked for this emission. */
        public int attemptedListeners() {
            return attemptedListeners;
        }

        /** Number of listeners that threw a RuntimeException. */
        public int failureCount() {
            return failures.size();
        }

        /** Returns true when every invoked listener returned normally. */
        public boolean successful() {
            return failures.isEmpty();
        }

        /** Listener failures in subscription/invocation order. */
        public List<RuntimeException> failures() {
            return failures;
        }
    }
}

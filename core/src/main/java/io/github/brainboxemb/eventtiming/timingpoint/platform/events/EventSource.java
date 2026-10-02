package io.github.brainboxemb.eventtiming.timingpoint.platform.events;

import java.util.function.Consumer;

/**
 * Subscription-only view of a local typed event.
 *
 * <p>The owner keeps the mutable {@link Event} so only that owner can decide
 * when a fact is emitted. Consumers receive this view and can only subscribe or
 * unsubscribe.</p>
 */
public interface EventSource<T> {
    boolean subscribe(Consumer<T> listener);

    boolean unsubscribe(Consumer<T> listener);
}

package io.github.brainboxemb.eventtiming.timingpoint.platform.events;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class EventTest {

    @Test
    public void emitsInSubscriptionOrderAndAvoidsDuplicateSubscription() {
        Event<String> event = new Event<>();
        List<String> delivered = new ArrayList<>();

        Consumer<String> first = value -> delivered.add("first:" + value);
        Consumer<String> second = value -> delivered.add("second:" + value);

        assertTrue(event.subscribe(first));
        assertFalse(event.subscribe(first));
        assertTrue(event.subscribe(second));

        Event.DeliveryReport report = event.emit("value");

        assertTrue(report.successful());
        assertEquals(2, report.attemptedListeners());
        assertEquals(
                Arrays.asList("first:value", "second:value"),
                delivered);
    }

    @Test
    public void unsubscribeStopsLaterDelivery() {
        Event<String> event = new Event<>();
        List<String> delivered = new ArrayList<>();
        Consumer<String> listener = delivered::add;

        event.subscribe(listener);
        assertTrue(event.unsubscribe(listener));
        assertFalse(event.unsubscribe(listener));

        Event.DeliveryReport report = event.emit("value");

        assertEquals(0, report.attemptedListeners());
        assertTrue(delivered.isEmpty());
    }

    @Test
    public void runtimeFailureIsReportedAndDoesNotBlockLaterListener() {
        Event<String> event = new Event<>();
        List<String> delivered = new ArrayList<>();
        RuntimeException failure = new IllegalStateException("expected listener failure");

        event.subscribe(value -> {
            throw failure;
        });
        event.subscribe(delivered::add);

        Event.DeliveryReport report = event.emit("value");

        assertFalse(report.successful());
        assertEquals(2, report.attemptedListeners());
        assertEquals(1, report.failureCount());
        assertEquals(failure, report.failures().get(0));
        assertEquals(Arrays.asList("value"), delivered);
    }

    @Test
    public void subscriptionOnlyViewUsesSameThreadSafeRegistry() {
        Event<String> event = new Event<>();
        EventSource<String> source = event;
        List<String> delivered = new ArrayList<>();

        assertTrue(source.subscribe(delivered::add));
        event.emit("value");

        assertEquals(Arrays.asList("value"), delivered);
    }

    @Test
    public void concurrentEmitsAreNotSerializedByEvent() throws Exception {
        Event<String> event = new Event<>();
        CountDownLatch entered = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);

        event.subscribe(value -> {
            entered.countDown();
            try {
                release.await();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ex);
            }
        });

        Thread first = new Thread(() -> event.emit("first"), "event-test-first");
        Thread second = new Thread(() -> event.emit("second"), "event-test-second");
        first.start();
        second.start();

        try {
            assertTrue(
                    "both emits should enter the listener concurrently",
                    entered.await(1, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            first.join(1000);
            second.join(1000);
        }

        assertFalse(first.isAlive());
        assertFalse(second.isAlive());
    }

    @Test
    public void fatalErrorIsNotSwallowed() {
        Event<String> event = new Event<>();
        AssertionError fatal = new AssertionError("expected fatal");
        event.subscribe(value -> {
            throw fatal;
        });

        try {
            event.emit("value");
            fail("expected fatal Error");
        } catch (AssertionError expected) {
            assertEquals(fatal, expected);
        }
    }
}

package io.github.brainboxemb.eventtiming.timingpoint.platform.events;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

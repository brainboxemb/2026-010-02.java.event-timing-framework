package io.github.brainboxemb.eventtiming.application;

import io.github.brainboxemb.eventtiming.domain.timing.TimingNode;
import io.github.brainboxemb.eventtiming.domain.timing.TimingNodeId;

import java.time.Instant;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ApplicationStatusTest {
    @Test
    public void exposesCurrentApplicationAndTimingNodeStatus() {
        Instant startedAt = Instant.parse("2026-09-25T13:00:00Z");
        ApplicationStatus status = new ApplicationStatus(
                "RUNNING",
                startedAt,
                new TimingNodeId("timing-node-01"),
                TimingNode.Lifecycle.CLOSED);

        assertEquals("RUNNING", status.applicationState());
        assertEquals(startedAt, status.startedAt());
        assertEquals("timing-node-01", status.timingNodeId().value());
        assertEquals(TimingNode.Lifecycle.CLOSED, status.timingNodeLifecycle());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankApplicationState() {
        new ApplicationStatus(
                " ",
                Instant.EPOCH,
                new TimingNodeId("timing-node-01"),
                TimingNode.Lifecycle.CLOSED);
    }
}

package io.github.brainboxemb.eventtiming.application;

import io.github.brainboxemb.eventtiming.domain.timing.TimingNode;
import io.github.brainboxemb.eventtiming.domain.timing.TimingNodeId;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ApplicationStatusTest {
    @Test
    public void exposesCurrentTimingNodeStatus() {
        ApplicationStatus status = new ApplicationStatus(
                new TimingNodeId("timing-node-01"),
                TimingNode.Lifecycle.CLOSED);

        assertEquals("timing-node-01", status.timingNodeId().value());
        assertEquals(TimingNode.Lifecycle.CLOSED, status.timingNodeLifecycle());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingTimingNodeId() {
        new ApplicationStatus(null, TimingNode.Lifecycle.CLOSED);
    }
}

package io.github.brainboxemb.eventtiming.timingpoint.application;

import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeTypes;
import io.github.brainboxemb.eventtiming.timingdata.TimingNodeId;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ApplicationStatusTest {
    @Test
    public void exposesCurrentTimingNodeStatus() {
        ApplicationStatus status = new ApplicationStatus(
                new TimingNodeId("timing-node-01"),
                TimingNodeTypes.Lifecycle.CLOSED);

        assertEquals("timing-node-01", status.timingNodeId().value());
        assertEquals(TimingNodeTypes.Lifecycle.CLOSED, status.timingNodeLifecycle());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingTimingNodeId() {
        new ApplicationStatus(null, TimingNodeTypes.Lifecycle.CLOSED);
    }
}

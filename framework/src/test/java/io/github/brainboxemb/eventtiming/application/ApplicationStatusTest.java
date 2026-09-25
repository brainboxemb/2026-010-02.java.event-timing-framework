package io.github.brainboxemb.eventtiming.application;

import io.github.brainboxemb.eventtiming.domain.timing.TimingNodeId;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ApplicationStatusTest {
    @Test
    public void exposesCurrentApplicationAndTimingNodeIdentity() {
        ApplicationStatus status =
                new ApplicationStatus("RUNNING", new TimingNodeId("timing-node-01"));

        assertEquals("RUNNING", status.applicationState());
        assertEquals("timing-node-01", status.timingNodeId().value());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankApplicationState() {
        new ApplicationStatus(" ", new TimingNodeId("timing-node-01"));
    }
}

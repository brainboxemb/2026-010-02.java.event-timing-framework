package io.github.brainboxemb.eventtiming.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimingNodeIdTest {
    @Test
    public void keepsStableIdentifierValue() {
        TimingNodeId id = new TimingNodeId("timing-node-01");

        assertEquals("timing-node-01", id.value());
        assertEquals(new TimingNodeId("timing-node-01"), id);
        assertEquals("timing-node-01", id.toString());
    }

    @Test
    public void trimsConfigurationWhitespace() {
        assertEquals("timing-node-01", new TimingNodeId("  timing-node-01  ").value());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankIdentifier() {
        new TimingNodeId("   ");
    }
}

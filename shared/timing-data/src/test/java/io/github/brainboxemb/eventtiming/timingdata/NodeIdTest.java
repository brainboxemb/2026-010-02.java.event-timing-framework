package io.github.brainboxemb.eventtiming.timingdata;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NodeIdTest {
    @Test
    public void keepsStableIdentifierValue() {
        TimingDataTypes.NodeId id = new TimingDataTypes.NodeId("TN-01");

        assertEquals("TN-01", id.value());
        assertEquals(new TimingDataTypes.NodeId("TN-01"), id);
        assertEquals("TN-01", id.toString());
    }

    @Test
    public void trimsConfigurationWhitespace() {
        assertEquals("TN-01", new TimingDataTypes.NodeId("  TN-01  ").value());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankIdentifier() {
        new TimingDataTypes.NodeId("   ");
    }
}

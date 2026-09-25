package io.github.brainboxemb.eventtiming.domain.timing;

import org.junit.Test;

import static org.junit.Assert.assertSame;

public class TimingNodeTest {
    @Test
    public void ownsConfiguredIdentity() {
        TimingNodeId id = new TimingNodeId("timing-node-01");

        TimingNode node = new TimingNode(id);

        assertSame(id, node.timingNodeId());
        assertSame(TimingNode.Lifecycle.CLOSED, node.lifecycle());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingIdentity() {
        new TimingNode(null);
    }
}

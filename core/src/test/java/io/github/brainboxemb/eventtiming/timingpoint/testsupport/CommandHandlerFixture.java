package io.github.brainboxemb.eventtiming.timingpoint.testsupport;

import io.github.brainboxemb.eventtiming.timingdata.TimingData;
import io.github.brainboxemb.eventtiming.timingdata.TimingNodeId;
import io.github.brainboxemb.eventtiming.timingdata.TimingTimestamp;
import io.github.brainboxemb.eventtiming.timingdata.defaultprofile.DefaultTimingDataFactory;
import io.github.brainboxemb.eventtiming.timingpoint.application.CommandHandler;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNode;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timingdata.TimingDataPersistence;
import io.github.brainboxemb.eventtiming.timingpoint.infra.BuildIdentity;

import java.util.Collections;

/**
 * Complete running TimingNode/CommandHandler composition for presentation tests.
 *
 * <p>This keeps test convenience out of the production CommandHandler API.</p>
 */
public final class CommandHandlerFixture implements AutoCloseable {
    private static final TimingTimestamp RECORDED_AT =
            TimingTimestamp.parse("2026-10-01T12:00:01.000000000Z");

    private final TimingNode node;
    private final CommandHandler handler;

    public CommandHandlerFixture(BuildIdentity identity) {
        node = new TimingNode(
                new TimingNodeId("timing-node-01"),
                new MemoryPersistence(),
                new DefaultTimingDataFactory(),
                () -> RECORDED_AT);
        handler = new CommandHandler(identity, node);
        node.start();
    }

    public CommandHandler handler() {
        return handler;
    }

    @Override
    public void close() {
        node.stop();
    }

    private static final class MemoryPersistence implements TimingDataPersistence {
        @Override
        public LoadResult load() {
            return new LoadResult(
                    Collections.<TimingData>emptyList(),
                    false);
        }

        @Override
        public void append(TimingData data) {
            // Presentation tests do not exercise durable persistence.
        }
    }
}

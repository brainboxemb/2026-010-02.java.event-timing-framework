package io.github.brainboxemb.eventtiming.timingpoint.domain.timing;

import io.github.brainboxemb.eventtiming.timingdata.LocationId;
import io.github.brainboxemb.eventtiming.timingdata.TimingData;
import io.github.brainboxemb.eventtiming.timingdata.TimingNodeId;
import io.github.brainboxemb.eventtiming.timingdata.TimingTimestamp;
import io.github.brainboxemb.eventtiming.timingdata.defaultprofile.DefaultTimingDataFactory;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timingdata.TimingDataPersistence;
import io.github.brainboxemb.eventtiming.timingpoint.platform.execution.SerialWorker;

import java.util.Collections;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TimingNodeTest {
    @Test
    public void startsClosedWithoutLocation() {
        TimingNodeId id = new TimingNodeId("timing-node-01");
        TimingNode node = node(id);

        node.start();
        try {
            TimingNodeTypes.Status status = node.query(TimingNodeQueries.status());

            assertSame(id, status.timingNodeId());
            assertEquals(TimingNodeTypes.Lifecycle.CLOSED, status.lifecycle());
            assertFalse(status.hasLocation());
        } finally {
            node.stop();
        }
    }

    @Test
    public void configuresLocationWhileClosedThenOpensAndCloses() {
        TimingNode node = node(new TimingNodeId("timing-node-01"));
        LocationId location = new LocationId(24);

        node.start();
        try {
            assertEquals(TimingNodeTypes.SetLocationResult.UPDATED, node.invoke(TimingNodeCommands.setLocation(location)));
            assertEquals(TimingNodeTypes.OpenResult.OPENED, node.invoke(TimingNodeCommands.open()));

            TimingNodeTypes.Status openStatus = node.query(TimingNodeQueries.status());
            assertEquals(TimingNodeTypes.Lifecycle.OPEN, openStatus.lifecycle());
            assertEquals(location, openStatus.locationId());

            assertEquals(
                    TimingNodeTypes.SetLocationResult.NODE_NOT_CLOSED,
                    node.invoke(TimingNodeCommands.setLocation(new LocationId(25))));
            assertEquals(location, node.query(TimingNodeQueries.status()).locationId());

            assertEquals(TimingNodeTypes.CloseResult.CLOSED, node.invoke(TimingNodeCommands.close()));

            TimingNodeTypes.Status closedStatus = node.query(TimingNodeQueries.status());
            assertEquals(TimingNodeTypes.Lifecycle.CLOSED, closedStatus.lifecycle());
            assertEquals(location, closedStatus.locationId());
        } finally {
            node.stop();
        }
    }

    @Test
    public void openWithoutLocationIsProcessedDomainRejection() {
        TimingNode node = node(new TimingNodeId("timing-node-01"));

        node.start();
        try {
            assertEquals(TimingNodeTypes.OpenResult.NO_LOCATION, node.invoke(TimingNodeCommands.open()));
            assertEquals(TimingNodeTypes.Lifecycle.CLOSED, node.query(TimingNodeQueries.status()).lifecycle());
        } finally {
            node.stop();
        }
    }

    @Test
    public void repeatedLifecycleCommandsReturnProcessedResults() {
        TimingNode node = node(new TimingNodeId("timing-node-01"));

        node.start();
        try {
            node.invoke(TimingNodeCommands.setLocation(new LocationId(24)));
            assertEquals(TimingNodeTypes.OpenResult.OPENED, node.invoke(TimingNodeCommands.open()));
            assertEquals(TimingNodeTypes.OpenResult.ALREADY_OPEN, node.invoke(TimingNodeCommands.open()));
            assertEquals(TimingNodeTypes.CloseResult.CLOSED, node.invoke(TimingNodeCommands.close()));
            assertEquals(TimingNodeTypes.CloseResult.ALREADY_CLOSED, node.invoke(TimingNodeCommands.close()));
        } finally {
            node.stop();
        }
    }

    @Test
    public void timeoutDoesNotCancelAcceptedOperation() throws Exception {
        SerialWorker worker = new SerialWorker(2, "timing-node-test");
        TimingNode node = node(
                new TimingNodeId("timing-node-01"),
                worker,
                25L);
        CountDownLatch blockerStarted = new CountDownLatch(1);
        CountDownLatch releaseBlocker = new CountDownLatch(1);

        node.start();
        try {
            worker.submit(() -> {
                blockerStarted.countDown();
                releaseBlocker.await();
                return null;
            });
            assertTrue(blockerStarted.await(1, TimeUnit.SECONDS));

            try {
                node.invoke(TimingNodeCommands.setLocation(new LocationId(24)));
                fail("expected timeout");
            } catch (TimingNodeTypes.OperationException expected) {
                assertEquals(
                        TimingNodeTypes.OperationException.Reason.TIMEOUT,
                        expected.reason());
            }

            CountDownLatch afterTimedOutOperation = new CountDownLatch(1);
            worker.offer(afterTimedOutOperation::countDown);
            releaseBlocker.countDown();

            assertTrue(afterTimedOutOperation.await(1, TimeUnit.SECONDS));
            TimingNodeTypes.Status status = node.query(TimingNodeQueries.status());
            assertTrue(status.hasLocation());
            assertEquals(new LocationId(24), status.locationId());
        } finally {
            releaseBlocker.countDown();
            node.stop();
        }
    }

    @Test
    public void stateDependentOperationsAreDecidedInQueueOrder() throws Exception {
        SerialWorker worker = new SerialWorker(4, "timing-node-test");
        TimingNode node = node(
                new TimingNodeId("timing-node-01"),
                worker,
                1000L);
        CountDownLatch blockerStarted = new CountDownLatch(1);
        CountDownLatch releaseBlocker = new CountDownLatch(1);

        node.start();
        try {
            assertEquals(
                    TimingNodeTypes.SetLocationResult.UPDATED,
                    node.invoke(TimingNodeCommands.setLocation(new LocationId(24))));

            worker.submit(() -> {
                blockerStarted.countDown();
                releaseBlocker.await();
                return null;
            });
            assertTrue(blockerStarted.await(1, TimeUnit.SECONDS));

            final TimingNodeTypes.OpenResult[] openResult = new TimingNodeTypes.OpenResult[1];
            final TimingNodeTypes.SetLocationResult[] setLocationResult =
                    new TimingNodeTypes.SetLocationResult[1];

            Thread openCaller = new Thread(() -> openResult[0] = node.invoke(TimingNodeCommands.open()));
            Thread locationCaller = new Thread(
                    () -> setLocationResult[0] = node.invoke(TimingNodeCommands.setLocation(new LocationId(25))));

            openCaller.start();
            long queueDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
            while (worker.queueDepth() < 1 && System.nanoTime() < queueDeadline) {
                Thread.yield();
            }
            assertEquals(1, worker.queueDepth());

            locationCaller.start();
            queueDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
            while (worker.queueDepth() < 2 && System.nanoTime() < queueDeadline) {
                Thread.yield();
            }
            assertEquals(2, worker.queueDepth());

            releaseBlocker.countDown();

            openCaller.join(1000);
            locationCaller.join(1000);
            assertFalse(openCaller.isAlive());
            assertFalse(locationCaller.isAlive());

            assertEquals(TimingNodeTypes.OpenResult.OPENED, openResult[0]);
            assertEquals(
                    TimingNodeTypes.SetLocationResult.NODE_NOT_CLOSED,
                    setLocationResult[0]);
            assertEquals(new LocationId(24), node.query(TimingNodeQueries.status()).locationId());
        } finally {
            releaseBlocker.countDown();
            node.stop();
        }
    }

    @Test
    public void submissionOnlyCommandReturnsAfterAdmissionWithoutWaitingForExecution()
            throws Exception {
        SerialWorker worker = new SerialWorker(2, "timing-node-submit-test");
        TimingNode node = node(
                new TimingNodeId("timing-node-01"),
                worker,
                1000L);
        CountDownLatch blockerStarted = new CountDownLatch(1);
        CountDownLatch releaseBlocker = new CountDownLatch(1);
        CountDownLatch producerReturned = new CountDownLatch(1);
        final TimingNodeTypes.CommandAdmission[] admission =
                new TimingNodeTypes.CommandAdmission[1];

        node.start();
        try {
            worker.submit(() -> {
                blockerStarted.countDown();
                releaseBlocker.await();
                return null;
            });
            assertTrue(blockerStarted.await(1, TimeUnit.SECONDS));

            Thread producer = new Thread(() -> {
                admission[0] =
                        node.submit(
                                TimingNodeCommands.setLocation(
                                        new LocationId(24)));
                producerReturned.countDown();
            });
            producer.start();

            assertTrue(
                    "submission-only producer must not wait for command execution",
                    producerReturned.await(250, TimeUnit.MILLISECONDS));
            assertEquals(
                    TimingNodeTypes.CommandAdmission.ACCEPTED,
                    admission[0]);

            releaseBlocker.countDown();
            producer.join(1000);
            assertFalse(producer.isAlive());

            CountDownLatch afterSubmittedCommand = new CountDownLatch(1);
            assertEquals(
                    SerialWorker.AdmissionResult.ACCEPTED,
                    worker.offer(afterSubmittedCommand::countDown));
            assertTrue(afterSubmittedCommand.await(1, TimeUnit.SECONDS));

            assertEquals(
                    new LocationId(24),
                    node.query(TimingNodeQueries.status()).locationId());
        } finally {
            releaseBlocker.countDown();
            node.stop();
        }
    }

    @Test
    public void operationBeforeStartIsUnavailable() {
        TimingNode node = node(new TimingNodeId("timing-node-01"));

        try {
            node.query(TimingNodeQueries.status());
            fail("expected operation failure");
        } catch (TimingNodeTypes.OperationException expected) {
            assertEquals(
                    TimingNodeTypes.OperationException.Reason.UNAVAILABLE,
                    expected.reason());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingIdentity() {
        new TimingNode(
                null,
                new NoOpPersistence(),
                new DefaultTimingDataFactory(),
                TimingNodeTest::now);
    }

    private static TimingNode node(TimingNodeId id) {
        return new TimingNode(
                id,
                new NoOpPersistence(),
                new DefaultTimingDataFactory(),
                TimingNodeTest::now);
    }

    /**
     * Creates a complete node while exposing worker/timeout control only to
     * these boundary tests. Production code never uses this construction path.
     */
    private static TimingNode node(
            TimingNodeId id,
            SerialWorker worker,
            long timeoutMillis) {
        TimingNodeLogic logic = new TimingNodeLogic(
                id,
                new NoOpPersistence(),
                new DefaultTimingDataFactory(),
                TimingNodeTest::now);
        return new TimingNode(logic, worker, timeoutMillis);
    }

    private static TimingTimestamp now() {
        return TimingTimestamp.parse("2026-10-02T08:00:00.000000000Z");
    }

    private static final class NoOpPersistence implements TimingDataPersistence {
        @Override
        public LoadResult load() {
            return new LoadResult(
                    Collections.<TimingData>emptyList(),
                    false);
        }

        @Override
        public void append(TimingData data) {
            // TimingNodeTest exercises execution/state behaviour, not persistence.
        }
    }
}

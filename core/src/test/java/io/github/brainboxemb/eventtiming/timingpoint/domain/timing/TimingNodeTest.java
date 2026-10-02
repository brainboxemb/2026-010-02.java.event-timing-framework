package io.github.brainboxemb.eventtiming.timingpoint.domain.timing;

import io.github.brainboxemb.eventtiming.timingdata.LocationId;
import io.github.brainboxemb.eventtiming.timingdata.TimingNodeId;
import io.github.brainboxemb.eventtiming.timingpoint.platform.execution.SerialWorker;

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
        TimingNode node = new TimingNode(id);

        node.start();
        try {
            TimingNode.Status status = node.query(TimingNodeQueries.status());

            assertSame(id, status.timingNodeId());
            assertEquals(TimingNode.Lifecycle.CLOSED, status.lifecycle());
            assertFalse(status.hasLocation());
        } finally {
            node.stop();
        }
    }

    @Test
    public void configuresLocationWhileClosedThenOpensAndCloses() {
        TimingNode node = new TimingNode(new TimingNodeId("timing-node-01"));
        LocationId location = new LocationId(24);

        node.start();
        try {
            assertEquals(TimingNode.SetLocationResult.UPDATED, node.invoke(TimingNodeCommands.setLocation(location)));
            assertEquals(TimingNode.OpenResult.OPENED, node.invoke(TimingNodeCommands.open()));

            TimingNode.Status openStatus = node.query(TimingNodeQueries.status());
            assertEquals(TimingNode.Lifecycle.OPEN, openStatus.lifecycle());
            assertEquals(location, openStatus.locationId());

            assertEquals(
                    TimingNode.SetLocationResult.NODE_NOT_CLOSED,
                    node.invoke(TimingNodeCommands.setLocation(new LocationId(25))));
            assertEquals(location, node.query(TimingNodeQueries.status()).locationId());

            assertEquals(TimingNode.CloseResult.CLOSED, node.invoke(TimingNodeCommands.close()));

            TimingNode.Status closedStatus = node.query(TimingNodeQueries.status());
            assertEquals(TimingNode.Lifecycle.CLOSED, closedStatus.lifecycle());
            assertEquals(location, closedStatus.locationId());
        } finally {
            node.stop();
        }
    }

    @Test
    public void openWithoutLocationIsProcessedDomainRejection() {
        TimingNode node = new TimingNode(new TimingNodeId("timing-node-01"));

        node.start();
        try {
            assertEquals(TimingNode.OpenResult.NO_LOCATION, node.invoke(TimingNodeCommands.open()));
            assertEquals(TimingNode.Lifecycle.CLOSED, node.query(TimingNodeQueries.status()).lifecycle());
        } finally {
            node.stop();
        }
    }

    @Test
    public void repeatedLifecycleCommandsReturnProcessedResults() {
        TimingNode node = new TimingNode(new TimingNodeId("timing-node-01"));

        node.start();
        try {
            node.invoke(TimingNodeCommands.setLocation(new LocationId(24)));
            assertEquals(TimingNode.OpenResult.OPENED, node.invoke(TimingNodeCommands.open()));
            assertEquals(TimingNode.OpenResult.ALREADY_OPEN, node.invoke(TimingNodeCommands.open()));
            assertEquals(TimingNode.CloseResult.CLOSED, node.invoke(TimingNodeCommands.close()));
            assertEquals(TimingNode.CloseResult.ALREADY_CLOSED, node.invoke(TimingNodeCommands.close()));
        } finally {
            node.stop();
        }
    }

    @Test
    public void timeoutDoesNotCancelAcceptedOperation() throws Exception {
        SerialWorker worker = new SerialWorker(2, "timing-node-test");
        TimingNode node = new TimingNode(
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
            } catch (TimingNode.OperationTimeoutException expected) {
                assertEquals(
                        TimingNode.OperationException.Reason.TIMEOUT,
                        expected.reason());
            }

            CountDownLatch afterTimedOutOperation = new CountDownLatch(1);
            worker.offer(afterTimedOutOperation::countDown);
            releaseBlocker.countDown();

            assertTrue(afterTimedOutOperation.await(1, TimeUnit.SECONDS));
            TimingNode.Status status = node.query(TimingNodeQueries.status());
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
        TimingNode node = new TimingNode(
                new TimingNodeId("timing-node-01"),
                worker,
                1000L);
        CountDownLatch blockerStarted = new CountDownLatch(1);
        CountDownLatch releaseBlocker = new CountDownLatch(1);

        node.start();
        try {
            assertEquals(
                    TimingNode.SetLocationResult.UPDATED,
                    node.invoke(TimingNodeCommands.setLocation(new LocationId(24))));

            worker.submit(() -> {
                blockerStarted.countDown();
                releaseBlocker.await();
                return null;
            });
            assertTrue(blockerStarted.await(1, TimeUnit.SECONDS));

            final TimingNode.OpenResult[] openResult = new TimingNode.OpenResult[1];
            final TimingNode.SetLocationResult[] setLocationResult =
                    new TimingNode.SetLocationResult[1];

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

            assertEquals(TimingNode.OpenResult.OPENED, openResult[0]);
            assertEquals(
                    TimingNode.SetLocationResult.NODE_NOT_CLOSED,
                    setLocationResult[0]);
            assertEquals(new LocationId(24), node.query(TimingNodeQueries.status()).locationId());
        } finally {
            releaseBlocker.countDown();
            node.stop();
        }
    }

    @Test
    public void operationBeforeStartIsUnavailable() {
        TimingNode node = new TimingNode(new TimingNodeId("timing-node-01"));

        try {
            node.query(TimingNodeQueries.status());
            fail("expected operation failure");
        } catch (TimingNode.OperationException expected) {
            assertEquals(
                    TimingNode.OperationException.Reason.UNAVAILABLE,
                    expected.reason());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingIdentity() {
        new TimingNode(null);
    }
}

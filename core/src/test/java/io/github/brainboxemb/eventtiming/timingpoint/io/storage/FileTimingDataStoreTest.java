package io.github.brainboxemb.eventtiming.timingpoint.io.storage;

import io.github.brainboxemb.eventtiming.timingdata.LocationId;
import io.github.brainboxemb.eventtiming.timingdata.RegistrationId;
import io.github.brainboxemb.eventtiming.timingdata.TimingData;
import io.github.brainboxemb.eventtiming.timingdata.TimingNodeId;
import io.github.brainboxemb.eventtiming.timingdata.TimingDataFactory;
import io.github.brainboxemb.eventtiming.timingdata.TimingTimestamp;
import io.github.brainboxemb.eventtiming.timingdata.defaultprofile.DefaultTimingDataCodec;
import io.github.brainboxemb.eventtiming.timingdata.defaultprofile.DefaultTimingDataFactory;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timingdata.TimingDataStore;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FileTimingDataStoreTest {
    private static final TimingNodeId NODE_ID = new TimingNodeId("timing-node-01");
    private static final TimingTimestamp EFFECTIVE =
            TimingTimestamp.parse("2026-10-01T12:00:00.000000000Z");
    private static final TimingTimestamp RECORDED =
            TimingTimestamp.parse("2026-10-01T12:00:01.000000000Z");

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final DefaultTimingDataFactory factory = new DefaultTimingDataFactory();
    private final DefaultTimingDataCodec codec = new DefaultTimingDataCodec();

    @Test
    public void appendWritesOneCanonicalLfTerminatedRecordAndLoadsIt() throws Exception {
        Path file = file();
        FileTimingDataStore store = store(file);
        TimingData first = data(NODE_ID, 1L);

        store.append(first);

        byte[] expected = concat(codec.encode(first), new byte[] {'\n'});
        assertArrayEquals(expected, Files.readAllBytes(file));

        TimingDataStore.LoadResult load = store.load();
        assertFalse(load.repairedIncompleteTail());
        assertEquals(1, load.records().size());
        assertEquals(1L, load.records().get(0).sequenceNumber());
    }

    @Test
    public void loadAcceptsCrLfFraming() throws Exception {
        Path file = file();
        TimingData first = data(NODE_ID, 1L);
        TimingData second = data(NODE_ID, 2L);
        Files.write(
                file,
                concat(
                        codec.encode(first),
                        new byte[] {'\r', '\n'},
                        codec.encode(second),
                        new byte[] {'\r', '\n'}));

        TimingDataStore.LoadResult load = store(file).load();

        assertFalse(load.repairedIncompleteTail());
        assertEquals(2, load.records().size());
        assertEquals(2L, load.records().get(1).sequenceNumber());
    }

    @Test
    public void loadTruncatesOnlyIncompleteTrailingRecord() throws Exception {
        Path file = file();
        TimingData first = data(NODE_ID, 1L);
        TimingData second = data(NODE_ID, 2L);
        byte[] firstLine = concat(codec.encode(first), new byte[] {'\n'});
        byte[] incompleteSecond = codec.encode(second);
        incompleteSecond = Arrays.copyOf(
                incompleteSecond,
                incompleteSecond.length - 7);
        Files.write(file, concat(firstLine, incompleteSecond));

        TimingDataStore.LoadResult load = store(file).load();

        assertTrue(load.repairedIncompleteTail());
        assertEquals(1, load.records().size());
        assertArrayEquals(firstLine, Files.readAllBytes(file));
    }

    @Test
    public void loadTreatsCompletelyUnterminatedFileAsIncompleteTail() throws Exception {
        Path file = file();
        Files.write(file, codec.encode(data(NODE_ID, 1L)));

        TimingDataStore.LoadResult load = store(file).load();

        assertTrue(load.repairedIncompleteTail());
        assertTrue(load.records().isEmpty());
        assertEquals(0L, Files.size(file));
    }

    @Test
    public void corruptCompleteRecordStopsRecovery() throws Exception {
        Path file = file();
        TimingData first = data(NODE_ID, 1L);
        Files.write(
                file,
                concat(
                        codec.encode(first),
                        new byte[] {'\n'},
                        "{not-json}\n".getBytes(StandardCharsets.UTF_8)));

        assertLoadFails(file, "line 2");
    }

    @Test
    public void sequenceGapStopsRecovery() throws Exception {
        Path file = file();
        Files.write(
                file,
                concat(
                        codec.encode(data(NODE_ID, 1L)),
                        new byte[] {'\n'},
                        codec.encode(data(NODE_ID, 3L)),
                        new byte[] {'\n'}));

        assertLoadFails(file, "sequence must be 2");
    }

    @Test
    public void differentTimingNodeStopsRecovery() throws Exception {
        Path file = file();
        Files.write(
                file,
                concat(
                        codec.encode(data(new TimingNodeId("timing-node-02"), 1L)),
                        new byte[] {'\n'}));

        assertLoadFails(file, "but store owns " + NODE_ID);
    }

    @Test
    public void blankCompleteLineIsInvalidRatherThanSkipped() throws Exception {
        Path file = file();
        Files.write(file, new byte[] {'\n'});

        assertLoadFails(file, "blank TimingData record");
    }

    @Test
    public void appendRejectsDifferentTimingNode() throws Exception {
        FileTimingDataStore store = store(file());

        try {
            store.append(data("timing-node-02", 1L));
            fail("expected wrong TimingNode rejection");
        } catch (TimingDataStore.StoreException expected) {
            assertTrue(expected.getMessage().contains("but store owns " + NODE_ID));
        }
    }

    private Path file() {
        return temporaryFolder.getRoot().toPath().resolve("timing-data.jsonl");
    }

    private FileTimingDataStore store(Path file) {
        return new FileTimingDataStore(file, NODE_ID, codec);
    }

    private TimingData data(TimingNodeId nodeId, long sequence) {
        return factory.createManualRegistration(
                new TimingDataFactory.Context(
                        nodeId,
                        sequence,
                        new LocationId(24),
                        EFFECTIVE,
                        RECORDED),
                new RegistrationId("registration-" + sequence),
                TimingData.ManualTimeSource.SYSTEM_ASSIGNED);
    }

    private void assertLoadFails(Path file, String expectedMessage) throws Exception {
        try {
            store(file).load();
            fail("expected recovery failure");
        } catch (TimingDataStore.StoreException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains(expectedMessage));
        }
    }

    private static byte[] concat(byte[]... parts) {
        int size = 0;
        for (byte[] part : parts) {
            size += part.length;
        }
        byte[] result = new byte[size];
        int offset = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, result, offset, part.length);
            offset += part.length;
        }
        return result;
    }
}

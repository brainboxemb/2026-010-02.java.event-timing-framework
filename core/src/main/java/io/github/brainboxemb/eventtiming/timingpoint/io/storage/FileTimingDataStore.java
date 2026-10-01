package io.github.brainboxemb.eventtiming.timingpoint.io.storage;

import io.github.brainboxemb.eventtiming.timingdata.TimingData;
import io.github.brainboxemb.eventtiming.timingdata.TimingDataCodec;
import io.github.brainboxemb.eventtiming.timingdata.TimingNodeId;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timingdata.TimingDataStore;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Append-only JSON Lines TimingDataStore for one configured TimingNode stream.
 *
 * <p>The supplied {@link TimingDataCodec} encodes and decodes one record payload.
 * This store adds the canonical LF record boundary, validates stream identity and
 * contiguous sequence during recovery, and repairs only an unterminated final
 * record.</p>
 *
 * <p>Append flow:</p>
 *
 * <pre>{@code
 * byte[] payload = codec.encode(data);
 * write(payload);
 * write(LF);
 * fileChannel.force(true);
 * // append() returns only after force(true)
 * }</pre>
 *
 * <p>If writing fails after only part of the final record reached the file,
 * {@link #load()} can truncate that incomplete unterminated tail on the next
 * startup. A corrupt <em>complete</em> line, wrong TimingNode identity or
 * sequence gap is never skipped or renumbered.</p>
 *
 * <p>{@code FileChannel.force(true)} is the first Java durability primitive.
 * Target-filesystem/power-loss qualification remains necessary before making
 * stronger deployment-specific guarantees.</p>
 */
public final class FileTimingDataStore implements TimingDataStore {
    private static final byte LF = (byte) '\n';
    private static final byte CR = (byte) '\r';

    private final Path file;
    private final TimingNodeId timingNodeId;
    private final TimingDataCodec codec;

    public FileTimingDataStore(
            Path file,
            TimingNodeId timingNodeId,
            TimingDataCodec codec) {
        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }
        if (timingNodeId == null) {
            throw new IllegalArgumentException("timingNodeId must not be null");
        }
        if (codec == null) {
            throw new IllegalArgumentException("codec must not be null");
        }
        this.file = file;
        this.timingNodeId = timingNodeId;
        this.codec = codec;
    }

    /**
     * Loads and validates all complete records.
     *
     * <p>The first implementation intentionally reads the complete file into
     * memory. Expected event files are small enough for this to keep recovery
     * deterministic and easy to validate. A streaming implementation can replace
     * it later without changing this store contract if target measurement requires it.</p>
     */
    @Override
    public LoadResult load() throws StoreException {
        if (!Files.exists(file)) {
            return new LoadResult(Collections.emptyList(), false);
        }

        final byte[] bytes;
        try {
            bytes = Files.readAllBytes(file);
        } catch (IOException ex) {
            throw new StoreException("could not read TimingData file " + file, ex);
        }

        if (bytes.length == 0) {
            return new LoadResult(Collections.emptyList(), false);
        }

        int completeLength = lastCompleteBoundary(bytes);
        boolean incompleteTail = completeLength < bytes.length;

        List<TimingData> records = decodeCompleteRecords(bytes, completeLength);

        if (incompleteTail) {
            truncateToCompleteBoundary(completeLength);
        }

        return new LoadResult(records, incompleteTail);
    }

    @Override
    public void append(TimingData data) throws StoreException {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        if (!timingNodeId.equals(data.timingNodeId())) {
            throw new StoreException(
                    "TimingData belongs to " + data.timingNodeId()
                            + " but store owns " + timingNodeId);
        }

        final byte[] payload;
        try {
            payload = codec.encode(data);
        } catch (TimingDataCodec.CodecException ex) {
            throw new StoreException("could not encode TimingData for append", ex);
        }

        rejectEmbeddedLineEnding(payload);

        byte[] framed = Arrays.copyOf(payload, payload.length + 1);
        framed[framed.length - 1] = LF;

        try (FileChannel channel = FileChannel.open(
                file,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND)) {
            ByteBuffer buffer = ByteBuffer.wrap(framed);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        } catch (IOException ex) {
            throw new StoreException("could not durably append TimingData to " + file, ex);
        }
    }

    private List<TimingData> decodeCompleteRecords(
            byte[] bytes,
            int completeLength)
            throws StoreException {
        if (completeLength == 0) {
            return Collections.emptyList();
        }

        List<TimingData> records = new ArrayList<>();
        int start = 0;
        int lineNumber = 1;

        for (int i = 0; i < completeLength; i++) {
            if (bytes[i] != LF) {
                continue;
            }

            int end = i;
            if (end > start && bytes[end - 1] == CR) {
                end--;
            }
            if (end == start) {
                throw new StoreException(
                        "blank TimingData record at line " + lineNumber);
            }
            for (int p = start; p < end; p++) {
                if (bytes[p] == CR) {
                    throw new StoreException(
                            "unexpected CR inside TimingData record at line "
                                    + lineNumber);
                }
            }

            TimingData data = decodeLine(
                    Arrays.copyOfRange(bytes, start, end),
                    lineNumber);
            validateStreamRecord(data, records.size() + 1L, lineNumber);
            records.add(data);

            start = i + 1;
            lineNumber++;
        }

        return records;
    }

    private TimingData decodeLine(byte[] payload, int lineNumber)
            throws StoreException {
        try {
            return codec.decode(payload);
        } catch (TimingDataCodec.CodecException ex) {
            throw new StoreException(
                    "invalid complete TimingData record at line " + lineNumber,
                    ex);
        }
    }

    private void validateStreamRecord(
            TimingData data,
            long expectedSequence,
            int lineNumber)
            throws StoreException {
        if (!timingNodeId.equals(data.timingNodeId())) {
            throw new StoreException(
                    "TimingData line " + lineNumber + " belongs to "
                            + data.timingNodeId() + " but store owns " + timingNodeId);
        }
        if (data.sequenceNumber() != expectedSequence) {
            throw new StoreException(
                    "TimingData line " + lineNumber + " sequence must be "
                            + expectedSequence + " but was " + data.sequenceNumber());
        }
    }

    private void truncateToCompleteBoundary(int completeLength)
            throws StoreException {
        try (FileChannel channel = FileChannel.open(
                file,
                StandardOpenOption.WRITE)) {
            channel.truncate(completeLength);
            channel.force(true);
        } catch (IOException ex) {
            throw new StoreException(
                    "could not truncate incomplete TimingData tail in " + file,
                    ex);
        }
    }

    private static int lastCompleteBoundary(byte[] bytes) {
        for (int i = bytes.length - 1; i >= 0; i--) {
            if (bytes[i] == LF) {
                return i + 1;
            }
        }
        return 0;
    }

    private static void rejectEmbeddedLineEnding(byte[] payload)
            throws StoreException {
        if (payload == null || payload.length == 0) {
            throw new StoreException("TimingData codec returned an empty payload");
        }
        for (byte value : payload) {
            if (value == LF || value == CR) {
                throw new StoreException(
                        "TimingData codec payload must contain exactly one physical line");
            }
        }
    }
}

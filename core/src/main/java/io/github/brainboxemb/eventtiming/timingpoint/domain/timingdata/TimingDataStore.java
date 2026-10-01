package io.github.brainboxemb.eventtiming.timingpoint.domain.timingdata;

import io.github.brainboxemb.eventtiming.timingdata.TimingData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Durable persistence boundary for one TimingNode's authoritative TimingData stream.
 *
 * <p>The TimingNode commit path calls {@link #append(TimingData)} before adding
 * the same immutable value to LogBook. Startup/recovery code calls {@link #load()}
 * before accepting new registration work.</p>
 *
 * <p>Concrete stores own record framing and durability mechanics. The configured
 * {@code TimingDataCodec} owns only one record payload representation.</p>
 */
public interface TimingDataStore {

    /**
     * Loads the complete committed stream in sequence order.
     *
     * @return immutable load result, including whether an incomplete trailing
     *         write was repaired
     * @throws StoreException when a complete record or stream invariant is invalid
     */
    LoadResult load() throws StoreException;

    /**
     * Durably appends one complete TimingData record before returning.
     *
     * <p>Returning normally is the persistence gate used by TimingNode before
     * LogBook visibility.</p>
     */
    void append(TimingData data) throws StoreException;

    /**
     * Immutable startup/recovery result.
     *
     * <p>{@link #repairedIncompleteTail()} is deliberately explicit so status or
     * diagnostics can later report that recovery modified the persistence file.</p>
     */
    final class LoadResult {
        private final List<TimingData> records;
        private final boolean repairedIncompleteTail;

        public LoadResult(
                List<TimingData> records,
                boolean repairedIncompleteTail) {
            if (records == null) {
                throw new IllegalArgumentException("records must not be null");
            }
            List<TimingData> copy = new ArrayList<>(records.size());
            for (TimingData record : records) {
                if (record == null) {
                    throw new IllegalArgumentException("records must not contain null");
                }
                copy.add(record);
            }
            this.records = Collections.unmodifiableList(copy);
            this.repairedIncompleteTail = repairedIncompleteTail;
        }

        public List<TimingData> records() {
            return records;
        }

        public boolean repairedIncompleteTail() {
            return repairedIncompleteTail;
        }
    }

    /** Persistence/recovery failure exposed to the TimingNode/application boundary. */
    final class StoreException extends Exception {
        public StoreException(String message) {
            super(message);
        }

        public StoreException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

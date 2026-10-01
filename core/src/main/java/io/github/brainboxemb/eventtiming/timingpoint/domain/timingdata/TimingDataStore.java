package io.github.brainboxemb.eventtiming.timingpoint.domain.timingdata;

import io.github.brainboxemb.eventtiming.timingdata.TimingData;

/**
 * Durable append boundary for authoritative committed TimingData.
 *
 * <p>Loading and recovery are added with the concrete persistence slice. The
 * first contract states only the commit gate needed before LogBook visibility.</p>
 */
public interface TimingDataStore {

    void append(TimingData data) throws StoreException;

    final class StoreException extends Exception {
        public StoreException(String message) {
            super(message);
        }

        public StoreException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

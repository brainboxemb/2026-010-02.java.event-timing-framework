package io.github.brainboxemb.eventtiming.timingdata;

import java.util.Objects;

/**
 * Canonical registration identity carried by committed registration TimingData.
 *
 * <p>TagId and TeamId belong to source/reference domains and are resolved to
 * this value before TimingData construction. This value is also distinct from
 * the TimingData record key (TimingNodeId + sequence number).</p>
 */
public final class RegistrationId {
    private final String value;

    public RegistrationId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("value must not be blank");
        }
        this.value = value;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RegistrationId)) {
            return false;
        }
        RegistrationId that = (RegistrationId) other;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

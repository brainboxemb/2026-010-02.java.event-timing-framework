package io.github.brainboxemb.eventtiming.timingdata;

import java.util.Objects;

/**
 * Shared representation of the registration identity carried by registration TimingData.
 *
 * <p>The identifier value is intentionally opaque to the shared library. Its
 * event/profile-specific meaning, allowed value set and mapping from source
 * identities such as TagId or TeamId belong to the active event/reference
 * configuration.</p>
 *
 * <p>This class enforces only the common structural invariant that the serialized
 * value is not blank. It remains distinct from the TimingData record key
 * (TimingNodeId + sequence number).</p>
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

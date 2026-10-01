package io.github.brainboxemb.eventtiming.timingdata;

import java.util.Objects;

/**
 * Canonical provider-neutral participant identity carried by IF-05 TimingData.
 *
 * <p>The public TimingData contract deliberately does not encode event-specific
 * categories, number ranges, source formats or deployment mapping rules. Those
 * concerns belong to the provider/reference-data boundary that resolves this
 * value before commit.</p>
 */
public final class RegistrationIdentity {
    private final String value;

    public RegistrationIdentity(String value) {
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
        if (!(other instanceof RegistrationIdentity)) {
            return false;
        }
        RegistrationIdentity that = (RegistrationIdentity) other;
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

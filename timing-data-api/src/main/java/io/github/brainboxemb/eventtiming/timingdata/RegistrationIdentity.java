package io.github.brainboxemb.eventtiming.timingdata;

import java.util.Objects;

/** Canonical IF-05 participant identity used by registration records. */
public final class RegistrationIdentity {
    public static final int MIN_NUMBER = 1;
    public static final int MAX_NUMBER = 350;

    public enum Type {
        STANDARD,
        WOMEN,
        MEN
    }

    private final Type type;
    private final int number;

    public RegistrationIdentity(Type type, int number) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (number < MIN_NUMBER || number > MAX_NUMBER) {
            throw new IllegalArgumentException(
                    "number must be in range " + MIN_NUMBER + ".." + MAX_NUMBER);
        }
        this.type = type;
        this.number = number;
    }

    public Type type() {
        return type;
    }

    public int number() {
        return number;
    }

    /**
     * Returns whether this identity is valid at the supplied IF-05 location.
     *
     * <p>STANDARD uses locations 1..23, WOMEN uses 24 and MEN uses 25.</p>
     */
    public boolean supportsLocationId(int locationId) {
        switch (type) {
            case STANDARD:
                return locationId >= 1 && locationId <= 23;
            case WOMEN:
                return locationId == 24;
            case MEN:
                return locationId == 25;
            default:
                throw new IllegalStateException("Unsupported registration identity type: " + type);
        }
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
        return number == that.number && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, number);
    }

    @Override
    public String toString() {
        return type + ":" + number;
    }
}

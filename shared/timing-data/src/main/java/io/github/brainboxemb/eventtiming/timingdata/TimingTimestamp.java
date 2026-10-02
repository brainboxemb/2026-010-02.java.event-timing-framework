package io.github.brainboxemb.eventtiming.timingdata;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.Locale;

/**
 * Absolute IF-05 timestamp with compact canonical UTC text.
 *
 * <p>The canonical text uses a literal {@code Z} and carries only the
 * fractional-second digits needed to represent the instant, from zero through
 * nine digits.</p>
 */
public final class TimingTimestamp implements Comparable<TimingTimestamp> {
    private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseSensitive()
            .appendPattern("uuuu-MM-dd'T'HH:mm:ss")
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .appendLiteral('Z')
            .toFormatter(Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);

    private final Instant instant;

    public TimingTimestamp(Instant instant) {
        if (instant == null) {
            throw new IllegalArgumentException("instant must not be null");
        }
        this.instant = instant;
    }

    public static TimingTimestamp parse(String text) {
        if (text == null) {
            throw new IllegalArgumentException("timestamp text must not be null");
        }
        try {
            LocalDateTime local = LocalDateTime.parse(text, FORMATTER);
            return new TimingTimestamp(local.toInstant(ZoneOffset.UTC));
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "timestamp must use canonical UTC form yyyy-MM-ddTHH:mm:ss[.fraction]Z "
                            + "with 0..9 fractional digits",
                    ex);
        }
    }

    public Instant instant() {
        return instant;
    }

    @Override
    public int compareTo(TimingTimestamp other) {
        if (other == null) {
            throw new IllegalArgumentException("other must not be null");
        }
        return instant.compareTo(other.instant);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TimingTimestamp)) {
            return false;
        }
        TimingTimestamp that = (TimingTimestamp) other;
        return instant.equals(that.instant);
    }

    @Override
    public int hashCode() {
        return instant.hashCode();
    }

    @Override
    public String toString() {
        return FORMATTER.format(LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
    }
}

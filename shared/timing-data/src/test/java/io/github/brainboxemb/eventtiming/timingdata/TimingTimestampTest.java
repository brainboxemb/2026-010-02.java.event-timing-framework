package io.github.brainboxemb.eventtiming.timingdata;

import java.time.Instant;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TimingTimestampTest {

    @Test
    public void parsesAndWritesCompactUtcText() {
        TimingTimestamp timestamp =
                TimingTimestamp.parse("2026-09-30T20:01:39.123000000Z");

        assertEquals(
                Instant.parse("2026-09-30T20:01:39.123Z"),
                timestamp.instant());
        assertEquals(
                "2026-09-30T20:01:39.123Z",
                timestamp.toString());
    }

    @Test
    public void writesWholeSecondWithoutFraction() {
        TimingTimestamp timestamp =
                new TimingTimestamp(Instant.parse("2026-09-30T20:01:39Z"));

        assertEquals(
                "2026-09-30T20:01:39Z",
                timestamp.toString());
    }

    @Test
    public void preservesRequiredFractionPrecision() {
        TimingTimestamp timestamp =
                TimingTimestamp.parse("2026-09-30T20:01:39.123456789Z");

        assertEquals(
                "2026-09-30T20:01:39.123456789Z",
                timestamp.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsOffsetInsteadOfCanonicalZuluTime() {
        TimingTimestamp.parse("2026-09-30T22:01:39.123+02:00");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMoreThanNanosecondPrecision() {
        TimingTimestamp.parse("2026-09-30T20:01:39.1234567891Z");
    }

    @Test
    public void comparesByAbsoluteInstant() {
        TimingTimestamp first =
                TimingTimestamp.parse("2026-09-30T20:01:39.123Z");
        TimingTimestamp second =
                TimingTimestamp.parse("2026-09-30T20:01:39.124Z");

        assertTrue(first.compareTo(second) < 0);
    }
}

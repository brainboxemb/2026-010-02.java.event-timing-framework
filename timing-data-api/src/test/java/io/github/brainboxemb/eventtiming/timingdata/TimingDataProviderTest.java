package io.github.brainboxemb.eventtiming.timingdata;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class TimingDataProviderTest {

    @Test
    public void providerExposesStableIdAndCreatesCodec() {
        final TimingDataCodec codec = new NoOpCodec();

        TimingDataProvider provider = new TimingDataProvider() {
            @Override
            public String id() {
                return "reference";
            }

            @Override
            public TimingDataCodec createCodec() {
                return codec;
            }
        };

        assertEquals("reference", provider.id());
        assertSame(codec, provider.createCodec());
    }

    private static final class NoOpCodec implements TimingDataCodec {
        @Override
        public byte[] encode(TimingDataRecord record) {
            throw new UnsupportedOperationException("not used by this SPI test");
        }

        @Override
        public TimingDataRecord decode(byte[] encodedRecord) {
            throw new UnsupportedOperationException("not used by this SPI test");
        }
    }
}

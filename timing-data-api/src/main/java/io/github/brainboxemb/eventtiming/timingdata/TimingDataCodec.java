package io.github.brainboxemb.eventtiming.timingdata;

/**
 * Translates one TimingData record payload to or from a concrete representation.
 *
 * <p>Record framing belongs to the caller. For the canonical IF-05 JSON Lines
 * format, for example, this codec handles one JSON record while the store owns
 * LF/CRLF framing and incomplete-tail handling.</p>
 */
public interface TimingDataCodec {

    byte[] encode(TimingDataRecord record) throws TimingDataCodecException;

    TimingDataRecord decode(byte[] encodedRecord) throws TimingDataCodecException;
}

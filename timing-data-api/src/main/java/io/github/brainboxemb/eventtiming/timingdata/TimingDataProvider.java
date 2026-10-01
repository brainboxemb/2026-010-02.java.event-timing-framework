package io.github.brainboxemb.eventtiming.timingdata;

/**
 * Provider SPI for a concrete TimingData representation.
 *
 * <p>The provider translates representation only. IF-05 owns the public
 * TimingData semantics. Discovery, duplicate-id validation and provider
 * configuration belong to application/bootstrap infrastructure.</p>
 */
public interface TimingDataProvider {

    String id();

    TimingDataCodec createCodec();
}

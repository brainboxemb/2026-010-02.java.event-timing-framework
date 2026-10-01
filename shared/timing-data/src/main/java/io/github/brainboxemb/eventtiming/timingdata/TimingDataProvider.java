package io.github.brainboxemb.eventtiming.timingdata;

/**
 * Provider SPI for a concrete TimingData representation.
 *
 * <p>The provider supplies one coherent concrete TimingData family: a
 * stateless factory plus its matching codec. IF-05 owns the common semantics.
 * Discovery, duplicate-id validation and provider configuration belong to
 * application/bootstrap infrastructure.</p>
 */
public interface TimingDataProvider {

    String id();

    TimingDataFactory createFactory();

    TimingDataCodec createCodec();
}

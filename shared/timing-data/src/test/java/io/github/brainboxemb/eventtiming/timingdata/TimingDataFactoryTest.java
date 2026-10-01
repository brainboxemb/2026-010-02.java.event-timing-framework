package io.github.brainboxemb.eventtiming.timingdata;

import io.github.brainboxemb.eventtiming.timingdata.defaultprofile.DefaultTimingDataFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;

public class TimingDataFactoryTest {
    private static final TimingTimestamp EFFECTIVE =
            TimingTimestamp.parse("2026-09-30T20:01:39.123000000Z");
    private static final TimingTimestamp RECORDED =
            TimingTimestamp.parse("2026-09-30T20:01:40.000000000Z");

    @Test
    public void defaultFactoryReturnsTypedAutomaticRegistration() {
        TimingDataContext context = context();
        RegistrationId registrationId = new RegistrationId("registration-0042");

        AutomaticRegistrationTimingData autoRegTD =
                new DefaultTimingDataFactory()
                        .createAutomaticRegistration(context, registrationId);

        assertCommonFields(autoRegTD);
        assertSame(registrationId, autoRegTD.registrationId());
    }

    @Test
    public void defaultFactoryReturnsTypedManualRegistration() {
        TimingDataContext context = context();
        RegistrationId registrationId = new RegistrationId("registration-0042");

        ManualRegistrationTimingData manRegTD =
                new DefaultTimingDataFactory().createManualRegistration(
                        context,
                        registrationId,
                        ManualRegistrationTimeSource.OPERATOR_ENTERED);

        assertCommonFields(manRegTD);
        assertSame(registrationId, manRegTD.registrationId());
        assertSame(ManualRegistrationTimeSource.OPERATOR_ENTERED, manRegTD.timeSource());
    }

    @Test
    public void alternateProfileCanReturnDifferentConcreteClassesThroughSameTypedApi() {
        TimingDataContext context = context();
        RegistrationId registrationId = new RegistrationId("registration-0042");
        TimingDataFactory defaultFactory = new DefaultTimingDataFactory();
        TimingDataFactory dummyFactory = new DummyEventTimingDataFactory();

        AutomaticRegistrationTimingData defaultAuto =
                defaultFactory.createAutomaticRegistration(context, registrationId);
        AutomaticRegistrationTimingData dummyAuto =
                dummyFactory.createAutomaticRegistration(context, registrationId);

        assertNotEquals(defaultAuto.getClass(), dummyAuto.getClass());
        assertEquals(defaultAuto.timingNodeId(), dummyAuto.timingNodeId());
        assertEquals(defaultAuto.sequenceNumber(), dummyAuto.sequenceNumber());
        assertSame(registrationId, dummyAuto.registrationId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void defaultFactoryRejectsMissingManualTimeSource() {
        new DefaultTimingDataFactory().createManualRegistration(
                context(),
                new RegistrationId("registration-0042"),
                null);
    }

    private static TimingDataContext context() {
        return new TimingDataContext("timing-node-01", 7L, 12, EFFECTIVE, RECORDED);
    }

    private static void assertCommonFields(TimingData data) {
        assertEquals("timing-node-01", data.timingNodeId());
        assertEquals(7L, data.sequenceNumber());
        assertEquals(12, data.locationId());
        assertSame(EFFECTIVE, data.effectiveTime());
        assertSame(RECORDED, data.recordedAt());
    }

    private static final class DummyEventTimingDataFactory implements TimingDataFactory {
        @Override
        public AutomaticRegistrationTimingData createAutomaticRegistration(
                TimingDataContext context,
                RegistrationId registrationId) {
            return new DummyAutomaticRegistrationTimingData(context, registrationId);
        }

        @Override
        public ManualRegistrationTimingData createManualRegistration(
                TimingDataContext context,
                RegistrationId registrationId,
                ManualRegistrationTimeSource timeSource) {
            return new DummyManualRegistrationTimingData(context, registrationId, timeSource);
        }
    }

    private static final class DummyAutomaticRegistrationTimingData
            implements AutomaticRegistrationTimingData {
        private final TimingDataContext context;
        private final RegistrationId registrationId;

        private DummyAutomaticRegistrationTimingData(
                TimingDataContext context,
                RegistrationId registrationId) {
            this.context = context;
            this.registrationId = registrationId;
        }

        @Override
        public String timingNodeId() {
            return context.timingNodeId();
        }

        @Override
        public long sequenceNumber() {
            return context.sequenceNumber();
        }

        @Override
        public int locationId() {
            return context.locationId();
        }

        @Override
        public TimingTimestamp effectiveTime() {
            return context.effectiveTime();
        }

        @Override
        public TimingTimestamp recordedAt() {
            return context.recordedAt();
        }

        @Override
        public RegistrationId registrationId() {
            return registrationId;
        }
    }

    private static final class DummyManualRegistrationTimingData
            implements ManualRegistrationTimingData {
        private final TimingDataContext context;
        private final RegistrationId registrationId;
        private final ManualRegistrationTimeSource timeSource;

        private DummyManualRegistrationTimingData(
                TimingDataContext context,
                RegistrationId registrationId,
                ManualRegistrationTimeSource timeSource) {
            this.context = context;
            this.registrationId = registrationId;
            this.timeSource = timeSource;
        }

        @Override
        public String timingNodeId() {
            return context.timingNodeId();
        }

        @Override
        public long sequenceNumber() {
            return context.sequenceNumber();
        }

        @Override
        public int locationId() {
            return context.locationId();
        }

        @Override
        public TimingTimestamp effectiveTime() {
            return context.effectiveTime();
        }

        @Override
        public TimingTimestamp recordedAt() {
            return context.recordedAt();
        }

        @Override
        public RegistrationId registrationId() {
            return registrationId;
        }

        @Override
        public ManualRegistrationTimeSource timeSource() {
            return timeSource;
        }
    }
}

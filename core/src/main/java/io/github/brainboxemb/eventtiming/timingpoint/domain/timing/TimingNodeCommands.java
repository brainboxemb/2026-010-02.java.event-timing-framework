package io.github.brainboxemb.eventtiming.timingpoint.domain.timing;

import io.github.brainboxemb.eventtiming.timingdata.TimingDataTypes.LocationId;
import io.github.brainboxemb.eventtiming.timingdata.TimingDataTypes.RegistrationId;
import io.github.brainboxemb.eventtiming.timingdata.TimingData.ManualTimeSource;
import io.github.brainboxemb.eventtiming.timingdata.TimingTimestamp;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeTypes.CloseResult;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeTypes.OpenResult;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeTypes.RegistrationResult;
import io.github.brainboxemb.eventtiming.timingpoint.domain.timing.TimingNodeTypes.SetLocationResult;

/**
 * Standard state-changing commands supported by TimingNode.
 *
 * <p>The factory validates command input at creation time. Execution remains the
 * responsibility of {@link TimingNode}, which applies the command to its
 * package-private logic on the serial lane.</p>
 */
public final class TimingNodeCommands {
    private static final TimingNodeCommand<OpenResult> OPEN =
            simple("open", TimingNodeLogic::open);
    private static final TimingNodeCommand<CloseResult> CLOSE =
            simple("close", TimingNodeLogic::close);

    private TimingNodeCommands() {
    }

    public static TimingNodeCommand<OpenResult> open() {
        return OPEN;
    }

    public static TimingNodeCommand<CloseResult> close() {
        return CLOSE;
    }

    public static TimingNodeCommand<SetLocationResult> setLocation(
            LocationId locationId) {
        if (locationId == null) {
            throw new IllegalArgumentException("locationId must not be null");
        }
        return simple(
                "setLocation",
                logic -> logic.setLocation(locationId));
    }

    public static TimingNodeCommand<RegistrationResult>
            commitAutomaticRegistration(
                    RegistrationId registrationId,
                    TimingTimestamp observationTime) {
        if (registrationId == null) {
            throw new IllegalArgumentException("registrationId must not be null");
        }
        if (observationTime == null) {
            throw new IllegalArgumentException("observationTime must not be null");
        }

        return registrationCommand(
                "commitAutomaticRegistration",
                logic -> logic.commitAutomaticRegistration(
                        registrationId,
                        observationTime));
    }

    public static TimingNodeCommand<RegistrationResult>
            commitManualRegistration(
                    RegistrationId registrationId,
                    TimingTimestamp effectiveTime,
                    ManualTimeSource registrationTimeSource) {
        if (registrationId == null) {
            throw new IllegalArgumentException("registrationId must not be null");
        }
        if (effectiveTime == null) {
            throw new IllegalArgumentException("effectiveTime must not be null");
        }
        if (registrationTimeSource == null) {
            throw new IllegalArgumentException(
                    "registrationTimeSource must not be null");
        }

        return registrationCommand(
                "commitManualRegistration",
                logic -> logic.commitManualRegistration(
                        registrationId,
                        effectiveTime,
                        registrationTimeSource));
    }

    private static <R> TimingNodeCommand<R> simple(
            String name,
            TimingNodeCommand.Action<R> action) {
        return new TimingNodeCommand<>(
                name,
                action,
                (node, result) -> result);
    }

    private static TimingNodeCommand<RegistrationResult>
            registrationCommand(
                    String name,
                    TimingNodeCommand.Action<RegistrationResult> action) {
        return new TimingNodeCommand<>(
                name,
                action,
                TimingNode::publishCommitted);
    }
}

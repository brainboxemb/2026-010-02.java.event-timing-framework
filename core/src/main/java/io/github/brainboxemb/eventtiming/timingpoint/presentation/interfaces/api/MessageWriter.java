package io.github.brainboxemb.eventtiming.timingpoint.presentation.interfaces.api;

import io.github.brainboxemb.eventtiming.timingdata.TimingData;
import io.github.brainboxemb.eventtiming.timingdata.TimingDataCodec;
import io.github.brainboxemb.eventtiming.timingpoint.application.ApplicationStatus;
import io.github.brainboxemb.eventtiming.timingpoint.application.CommandHandler;
import io.github.brainboxemb.eventtiming.timingpoint.infra.BuildIdentity;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Explicit JSON mapping for the IF-03 v1 contract. */
public final class MessageWriter {
    private MessageWriter() {
    }

    public static String version(BuildIdentity identity) {
        return "{"
                + "\"application\":" + quote(identity.application()) + ","
                + "\"version\":" + quote(identity.version()) + ","
                + "\"revision\":" + quote(identity.revision()) + ","
                + "\"sourceRef\":" + quote(identity.sourceRef()) + ","
                + "\"buildOrigin\":" + quote(identity.buildOrigin()) + ","
                + "\"dirty\":" + identity.dirty() + ","
                + "\"apiVersion\":" + quote(identity.apiVersion())
                + "}";
    }

    public static String status(ApplicationStatus status) {
        String location = status.hasLocation()
                ? Integer.toString(status.locationId().value())
                : "null";
        return "{"
                + "\"nodes\":[{"
                + "\"id\":" + quote(status.timingNodeId().value()) + ","
                + "\"locationId\":" + location + ","
                + "\"state\":" + quote(status.timingNodeLifecycle().name())
                + "}],"
                + "\"problems\":[]"
                + "}";
    }

    public static String capabilities(CommandHandler.Capabilities capabilities) {
        return "{"
                + "\"capabilities\":[{"
                + "\"id\":\"DIRECT_REGISTRATION_SIMULATION\","
                + "\"supported\":"
                + capabilities.directRegistrationSimulationSupported() + ","
                + "\"enabled\":"
                + capabilities.directRegistrationSimulationEnabled()
                + "}]}";
    }

    public static String result(String result) {
        return "{\"result\":" + quote(result) + "}";
    }

    public static String committedRegistration(TimingData data) {
        return "{\"seq\":" + data.sequenceNumber() + "}";
    }

    public static String logBookInfo(int count) {
        return "{"
                + "\"count\":" + count + ","
                + "\"first\":" + (count == 0 ? "null" : "1") + ","
                + "\"last\":" + (count == 0 ? "null" : Integer.toString(count))
                + "}";
    }

    public static String logBookPage(
            int count,
            Long next,
            List<TimingData> records,
            TimingDataCodec codec)
            throws TimingDataCodec.CodecException {
        StringBuilder json = new StringBuilder();
        json.append("{")
                .append("\"count\":").append(count).append(",")
                .append("\"next\":")
                .append(next == null ? "null" : Long.toString(next.longValue()))
                .append(",")
                .append("\"records\":[");

        for (int i = 0; i < records.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append(timingDataJson(records.get(i), codec));
        }
        json.append("]}");
        return json.toString();
    }

    public static String statusEvent(
            String eventType,
            java.time.Instant occurredAt,
            ApplicationStatus status) {
        if (eventType == null || eventType.trim().isEmpty()) {
            throw new IllegalArgumentException("eventType must not be blank");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt must not be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        return "{"
                + "\"eventType\":" + quote(eventType) + ","
                + "\"occurredAt\":" + quote(occurredAt.toString()) + ","
                + "\"payload\":" + status(status)
                + "}";
    }

    public static String timingDataEvent(
            java.time.Instant occurredAt,
            TimingData data,
            TimingDataCodec codec)
            throws TimingDataCodec.CodecException {
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt must not be null");
        }
        return "{"
                + "\"eventType\":\"TIMING_DATA_COMMITTED\","
                + "\"occurredAt\":" + quote(occurredAt.toString()) + ","
                + "\"payload\":" + timingDataJson(data, codec)
                + "}";
    }

    public static String error(String code, String message) {
        return "{"
                + "\"error\":{"
                + "\"code\":" + quote(code) + ","
                + "\"message\":" + quote(message)
                + "}}";
    }

    private static String timingDataJson(
            TimingData data,
            TimingDataCodec codec)
            throws TimingDataCodec.CodecException {
        return new String(codec.encode(data), StandardCharsets.UTF_8);
    }

    private static String quote(String value) {
        StringBuilder result = new StringBuilder(value.length() + 2);
        result.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"':
                    result.append("\\\"");
                    break;
                case '\\':
                    result.append("\\\\");
                    break;
                case '\b':
                    result.append("\\b");
                    break;
                case '\f':
                    result.append("\\f");
                    break;
                case '\n':
                    result.append("\\n");
                    break;
                case '\r':
                    result.append("\\r");
                    break;
                case '\t':
                    result.append("\\t");
                    break;
                default:
                    if (ch < 0x20) {
                        result.append("\\u");
                        String hex = Integer.toHexString(ch);
                        for (int pad = hex.length(); pad < 4; pad++) {
                            result.append('0');
                        }
                        result.append(hex);
                    } else {
                        result.append(ch);
                    }
            }
        }
        result.append('"');
        return result.toString();
    }
}

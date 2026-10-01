package io.github.brainboxemb.eventtiming.timingdata.defaultprofile;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.github.brainboxemb.eventtiming.timingdata.RegistrationId;
import io.github.brainboxemb.eventtiming.timingdata.TimingData;
import io.github.brainboxemb.eventtiming.timingdata.TimingDataCodec;
import io.github.brainboxemb.eventtiming.timingdata.TimingDataFactory;
import io.github.brainboxemb.eventtiming.timingdata.TimingTimestamp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Canonical IF-05 v1 JSON codec for the built-in default/reference profile.
 *
 * <p>The codec owns one JSON object only. It deliberately does not add or remove
 * JSON Lines terminators: file framing, incomplete-tail handling and recovery
 * belong to the TimingData store.</p>
 *
 * <p>The writer emits the common IF-05 fields in canonical order, followed by
 * registration-specific fields. The reader does not depend on object member
 * order and ignores additional members after safely skipping their JSON value.
 * Required v1 fields and their semantic values remain strictly validated.</p>
 *
 * <p>Example:</p>
 *
 * <pre>{@code
 * DefaultTimingDataFactory factory = new DefaultTimingDataFactory();
 * TimingDataCodec codec = new DefaultTimingDataCodec();
 *
 * TimingData data = factory.createManualRegistration(
 *         context,
 *         registrationId,
 *         TimingData.ManualTimeSource.OPERATOR_ENTERED);
 *
 * byte[] encodedRecord = codec.encode(data); // no trailing LF
 * TimingData decoded = codec.decode(encodedRecord);
 * }</pre>
 */
public final class DefaultTimingDataCodec implements TimingDataCodec {
    private static final int VERSION = 1;
    private static final String RECORD_TYPE_REGISTRATION = "REGISTRATION";
    private static final String ORIGIN_AUTOMATIC = "AUTOMATIC";
    private static final String ORIGIN_MANUAL = "MANUAL";
    private static final String TIME_SOURCE_OBSERVED = "OBSERVED";

    private final JsonFactory jsonFactory;
    private final TimingDataFactory timingDataFactory;

    /** Creates the canonical codec backed by the built-in default TimingData factory. */
    public DefaultTimingDataCodec() {
        this(new JsonFactory(), new DefaultTimingDataFactory());
    }

    DefaultTimingDataCodec(
            JsonFactory jsonFactory,
            TimingDataFactory timingDataFactory) {
        if (jsonFactory == null) {
            throw new IllegalArgumentException("jsonFactory must not be null");
        }
        if (timingDataFactory == null) {
            throw new IllegalArgumentException("timingDataFactory must not be null");
        }
        this.jsonFactory = jsonFactory;
        this.timingDataFactory = timingDataFactory;
    }

    /**
     * Encodes one semantic TimingData value as canonical compact UTF-8 JSON.
     *
     * <p>No line terminator is emitted. The caller that owns JSON Lines framing
     * appends LF after the complete encoded record has been accepted for append.</p>
     */
    @Override
    public byte[] encode(TimingData data) throws CodecException {
        if (data == null) {
            throw new CodecException(
                    CodecException.Reason.ENCODE_FAILURE,
                    "TimingData must not be null");
        }

        validateCommonForEncode(data);

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream(256);
            JsonGenerator generator = jsonFactory.createGenerator(output);
            try {
                generator.writeStartObject();
                generator.writeNumberField("version", VERSION);
                generator.writeStringField("timingNodeId", data.timingNodeId());
                generator.writeNumberField("sequenceNumber", data.sequenceNumber());
                generator.writeNumberField("locationId", data.locationId());
                generator.writeStringField("recordType", RECORD_TYPE_REGISTRATION);
                generator.writeStringField("effectiveTime", data.effectiveTime().toString());
                generator.writeStringField("recordedAt", data.recordedAt().toString());

                if (data instanceof TimingData.AutomaticRegistration) {
                    writeAutomatic(
                            generator,
                            (TimingData.AutomaticRegistration) data);
                } else if (data instanceof TimingData.ManualRegistration) {
                    writeManual(
                            generator,
                            (TimingData.ManualRegistration) data);
                } else {
                    throw new CodecException(
                            CodecException.Reason.ENCODE_FAILURE,
                            "Default profile cannot encode TimingData type "
                                    + data.getClass().getName());
                }

                generator.writeEndObject();
            } finally {
                generator.close();
            }
            return output.toByteArray();
        } catch (CodecException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw new CodecException(
                    CodecException.Reason.ENCODE_FAILURE,
                    "Could not encode TimingData as IF-05 v1 JSON",
                    ex);
        }
    }

    private static void validateCommonForEncode(TimingData data) throws CodecException {
        try {
            new TimingDataFactory.Context(
                    data.timingNodeId(),
                    data.sequenceNumber(),
                    data.locationId(),
                    data.effectiveTime(),
                    data.recordedAt());
        } catch (RuntimeException ex) {
            throw new CodecException(
                    CodecException.Reason.ENCODE_FAILURE,
                    "TimingData contains invalid common IF-05 values",
                    ex);
        }
    }

    private static void writeAutomatic(
            JsonGenerator generator,
            TimingData.AutomaticRegistration data)
            throws IOException, CodecException {
        RegistrationId registrationId = requireRegistrationId(data.registrationId());
        generator.writeStringField("registrationId", registrationId.value());
        generator.writeStringField("origin", ORIGIN_AUTOMATIC);
        generator.writeStringField("timeSource", TIME_SOURCE_OBSERVED);
    }

    private static void writeManual(
            JsonGenerator generator,
            TimingData.ManualRegistration data)
            throws IOException, CodecException {
        RegistrationId registrationId = requireRegistrationId(data.registrationId());
        TimingData.ManualTimeSource timeSource = data.timeSource();
        if (timeSource == null) {
            throw new CodecException(
                    CodecException.Reason.ENCODE_FAILURE,
                    "manual timeSource must not be null");
        }

        generator.writeStringField("registrationId", registrationId.value());
        generator.writeStringField("origin", ORIGIN_MANUAL);
        generator.writeStringField("timeSource", timeSource.name());
    }

    private static RegistrationId requireRegistrationId(RegistrationId registrationId)
            throws CodecException {
        if (registrationId == null) {
            throw new CodecException(
                    CodecException.Reason.ENCODE_FAILURE,
                    "registrationId must not be null");
        }
        return registrationId;
    }

    /**
     * Decodes one JSON object payload into the default/reference TimingData family.
     *
     * <p>Additional members are ignored as required by IF-05 v1 compatibility.
     * Unsupported major versions and unknown v1 record types remain distinct from
     * malformed/invalid input.</p>
     */
    @Override
    public TimingData decode(byte[] encodedRecord) throws CodecException {
        if (encodedRecord == null || encodedRecord.length == 0) {
            throw invalid("encoded record must not be empty");
        }
        if (hasUtf8Bom(encodedRecord)) {
            throw invalid("canonical IF-05 JSON must not contain a UTF-8 BOM");
        }

        try {
            DecodedFields fields = readFields(encodedRecord);
            return toTimingData(fields);
        } catch (CodecException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw new CodecException(
                    CodecException.Reason.INVALID_DATA,
                    "Could not decode IF-05 v1 JSON record",
                    ex);
        }
    }

    private DecodedFields readFields(byte[] encodedRecord)
            throws IOException, CodecException {
        DecodedFields fields = new DecodedFields();

        JsonParser parser = jsonFactory.createParser(encodedRecord);
        try {
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                throw invalid("TimingData record must be one JSON object");
            }

            while (parser.nextToken() != JsonToken.END_OBJECT) {
                if (parser.currentToken() != JsonToken.FIELD_NAME) {
                    throw invalid("expected JSON object member name");
                }

                String name = parser.currentName();
                JsonToken valueToken = parser.nextToken();
                if (valueToken == null) {
                    throw invalid("missing value for JSON member " + name);
                }

                switch (name) {
                    case "version":
                        fields.version = readInt(parser, valueToken, name, fields.versionSeen);
                        fields.versionSeen = true;
                        break;
                    case "timingNodeId":
                        fields.timingNodeId =
                                readString(parser, valueToken, name, fields.timingNodeIdSeen);
                        fields.timingNodeIdSeen = true;
                        break;
                    case "sequenceNumber":
                        fields.sequenceNumber =
                                readLong(parser, valueToken, name, fields.sequenceNumberSeen);
                        fields.sequenceNumberSeen = true;
                        break;
                    case "locationId":
                        fields.locationId =
                                readInt(parser, valueToken, name, fields.locationIdSeen);
                        fields.locationIdSeen = true;
                        break;
                    case "recordType":
                        fields.recordType =
                                readString(parser, valueToken, name, fields.recordTypeSeen);
                        fields.recordTypeSeen = true;
                        break;
                    case "effectiveTime":
                        fields.effectiveTimeText =
                                readString(parser, valueToken, name, fields.effectiveTimeSeen);
                        fields.effectiveTimeSeen = true;
                        break;
                    case "recordedAt":
                        fields.recordedAtText =
                                readString(parser, valueToken, name, fields.recordedAtSeen);
                        fields.recordedAtSeen = true;
                        break;
                    case "registrationId":
                        fields.registrationId =
                                readString(parser, valueToken, name, fields.registrationIdSeen);
                        fields.registrationIdSeen = true;
                        break;
                    case "origin":
                        fields.origin =
                                readString(parser, valueToken, name, fields.originSeen);
                        fields.originSeen = true;
                        break;
                    case "timeSource":
                        fields.timeSource =
                                readString(parser, valueToken, name, fields.timeSourceSeen);
                        fields.timeSourceSeen = true;
                        break;
                    default:
                        parser.skipChildren();
                        break;
                }
            }

            if (parser.nextToken() != null) {
                throw invalid("unexpected data after TimingData JSON object");
            }
        } finally {
            parser.close();
        }

        return fields;
    }

    private TimingData toTimingData(DecodedFields fields) throws CodecException {
        require(fields.versionSeen, "version");
        if (fields.version != VERSION) {
            throw CodecException.unsupportedVersion(
                    fields.version,
                    "unsupported IF-05 TimingData version " + fields.version);
        }

        TimingDataFactory.Context context = commonContext(fields);
        TimingData.RecordKey key = new TimingData.RecordKey(
                context.timingNodeId(),
                context.sequenceNumber());

        require(fields.recordTypeSeen, "recordType");
        if (!RECORD_TYPE_REGISTRATION.equals(fields.recordType)) {
            throw CodecException.unsupportedRecordType(
                    VERSION,
                    key,
                    context.locationId(),
                    fields.recordType,
                    context.effectiveTime(),
                    context.recordedAt(),
                    "unsupported IF-05 v1 recordType " + fields.recordType);
        }

        require(fields.registrationIdSeen, "registrationId");
        require(fields.originSeen, "origin");
        require(fields.timeSourceSeen, "timeSource");

        final RegistrationId registrationId;
        try {
            registrationId = new RegistrationId(fields.registrationId);
        } catch (RuntimeException ex) {
            throw invalid("registrationId is invalid", ex);
        }

        if (ORIGIN_AUTOMATIC.equals(fields.origin)) {
            if (!TIME_SOURCE_OBSERVED.equals(fields.timeSource)) {
                throw invalid(
                        "automatic registration requires timeSource "
                                + TIME_SOURCE_OBSERVED);
            }
            return timingDataFactory.createAutomaticRegistration(
                    context,
                    registrationId);
        }

        if (ORIGIN_MANUAL.equals(fields.origin)) {
            final TimingData.ManualTimeSource manualTimeSource;
            try {
                manualTimeSource = TimingData.ManualTimeSource.valueOf(fields.timeSource);
            } catch (IllegalArgumentException ex) {
                throw invalid(
                        "manual registration has invalid timeSource "
                                + fields.timeSource,
                        ex);
            }
            return timingDataFactory.createManualRegistration(
                    context,
                    registrationId,
                    manualTimeSource);
        }

        throw invalid("registration has invalid origin " + fields.origin);
    }

    private static TimingDataFactory.Context commonContext(DecodedFields fields)
            throws CodecException {
        require(fields.timingNodeIdSeen, "timingNodeId");
        require(fields.sequenceNumberSeen, "sequenceNumber");
        require(fields.locationIdSeen, "locationId");
        require(fields.effectiveTimeSeen, "effectiveTime");
        require(fields.recordedAtSeen, "recordedAt");

        try {
            return new TimingDataFactory.Context(
                    fields.timingNodeId,
                    fields.sequenceNumber,
                    fields.locationId,
                    TimingTimestamp.parse(fields.effectiveTimeText),
                    TimingTimestamp.parse(fields.recordedAtText));
        } catch (RuntimeException ex) {
            throw invalid("common IF-05 TimingData envelope is invalid", ex);
        }
    }

    private static String readString(
            JsonParser parser,
            JsonToken token,
            String name,
            boolean alreadySeen)
            throws CodecException {
        rejectDuplicate(name, alreadySeen);
        if (token != JsonToken.VALUE_STRING) {
            throw invalid(name + " must be a JSON string");
        }
        return parser.getText();
    }

    private static int readInt(
            JsonParser parser,
            JsonToken token,
            String name,
            boolean alreadySeen)
            throws IOException, CodecException {
        rejectDuplicate(name, alreadySeen);
        if (token != JsonToken.VALUE_NUMBER_INT) {
            throw invalid(name + " must be a JSON integer");
        }
        try {
            return parser.getIntValue();
        } catch (RuntimeException ex) {
            throw invalid(name + " is outside the supported integer range", ex);
        }
    }

    private static long readLong(
            JsonParser parser,
            JsonToken token,
            String name,
            boolean alreadySeen)
            throws IOException, CodecException {
        rejectDuplicate(name, alreadySeen);
        if (token != JsonToken.VALUE_NUMBER_INT) {
            throw invalid(name + " must be a JSON integer");
        }
        try {
            return parser.getLongValue();
        } catch (RuntimeException ex) {
            throw invalid(name + " is outside the supported integer range", ex);
        }
    }

    private static void rejectDuplicate(String name, boolean alreadySeen)
            throws CodecException {
        if (alreadySeen) {
            throw invalid("duplicate JSON member " + name);
        }
    }

    private static void require(boolean present, String name) throws CodecException {
        if (!present) {
            throw invalid("missing required JSON member " + name);
        }
    }

    private static boolean hasUtf8Bom(byte[] encodedRecord) {
        return encodedRecord.length >= 3
                && (encodedRecord[0] & 0xff) == 0xef
                && (encodedRecord[1] & 0xff) == 0xbb
                && (encodedRecord[2] & 0xff) == 0xbf;
    }

    private static CodecException invalid(String message) {
        return new CodecException(CodecException.Reason.INVALID_DATA, message);
    }

    private static CodecException invalid(String message, Throwable cause) {
        return new CodecException(
                CodecException.Reason.INVALID_DATA,
                message,
                cause);
    }

    /** Parsed known members before semantic construction; unknown members are skipped. */
    private static final class DecodedFields {
        private int version;
        private boolean versionSeen;
        private String timingNodeId;
        private boolean timingNodeIdSeen;
        private long sequenceNumber;
        private boolean sequenceNumberSeen;
        private int locationId;
        private boolean locationIdSeen;
        private String recordType;
        private boolean recordTypeSeen;
        private String effectiveTimeText;
        private boolean effectiveTimeSeen;
        private String recordedAtText;
        private boolean recordedAtSeen;
        private String registrationId;
        private boolean registrationIdSeen;
        private String origin;
        private boolean originSeen;
        private String timeSource;
        private boolean timeSourceSeen;
    }
}

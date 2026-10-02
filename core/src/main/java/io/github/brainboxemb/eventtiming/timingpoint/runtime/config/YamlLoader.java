package io.github.brainboxemb.eventtiming.timingpoint.runtime.config;

import io.github.brainboxemb.eventtiming.timingdata.TimingDataTypes.NodeId;
import io.github.brainboxemb.eventtiming.timingpoint.infra.logging.LoggingConfig;
import io.github.brainboxemb.eventtiming.timingpoint.infra.logging.LoggingFileConfig;
import io.github.brainboxemb.eventtiming.timingpoint.infra.logging.LoggingLevel;
import io.github.brainboxemb.eventtiming.timingpoint.infra.loggingserver.LoggingServerConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * Maps the default IF-11 YAML syntax into the runtime configuration model.
 *
 * <p>This loader belongs with the runtime configuration because it knows the
 * concrete application configuration schema. It is not a generic infrastructure
 * YAML utility.</p>
 */
public final class YamlLoader {
    private static final String TIMING_NODE_ID = "timingNodeId";
    private static final String PRESENTATION = "presentation";
    private static final String IO = "io";
    private static final String STORAGE = "storage";
    private static final String TIMING_DATA = "timingData";
    private static final String LOGGING = "logging";
    private static final String LEVEL = "level";
    private static final String FILE = "file";
    private static final String PATH = "path";
    private static final String ROTATE_BYTES = "rotateBytes";
    private static final String RETAINED_FILES = "retainedFiles";
    private static final String LIVE = "live";
    private static final String REMOTE_SHELL = "remoteShell";
    private static final String API = "api";
    private static final String HTTP = "http";
    private static final String WEB_SOCKET = "webSocket";
    private static final String BIND_ADDRESS = "bindAddress";
    private static final String PORT = "port";

    private YamlLoader() {
    }

    public static Config load(Path path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("config path must not be null");
        }

        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        Yaml yaml = new Yaml(new SafeConstructor(options));

        Object document;
        try (InputStream input = Files.newInputStream(path)) {
            document = yaml.load(input);
        } catch (YAMLException ex) {
            throw new IllegalArgumentException("Invalid YAML configuration: " + path, ex);
        }

        Map<?, ?> root = requireMapping(document, "configuration root");
        rejectUnknownFields(root, "configuration root", TIMING_NODE_ID, PRESENTATION, LOGGING, IO);

        if (!root.containsKey(TIMING_NODE_ID)) {
            throw new IllegalArgumentException(
                    "Missing required configuration field: " + TIMING_NODE_ID);
        }

        NodeId timingNodeId = new NodeId(
                requireString(root.get(TIMING_NODE_ID), TIMING_NODE_ID));
        return new Config(
                timingNodeId,
                mapPresentation(root.get(PRESENTATION)),
                mapLogging(root.get(LOGGING)),
                mapLoggingLive(root.get(LOGGING)),
                mapTimingDataPath(root.get(IO)));
    }

    private static Path mapTimingDataPath(Object rawIo) {
        if (rawIo == null) {
            throw new IllegalArgumentException(
                    "Missing required configuration field: " + IO);
        }

        Map<?, ?> io = requireMapping(rawIo, IO);
        rejectUnknownFields(io, IO, STORAGE);
        if (!io.containsKey(STORAGE)) {
            throw new IllegalArgumentException(
                    "Missing required configuration field: " + IO + "." + STORAGE);
        }

        String storageField = IO + "." + STORAGE;
        Map<?, ?> storage = requireMapping(io.get(STORAGE), storageField);
        rejectUnknownFields(storage, storageField, TIMING_DATA);
        if (!storage.containsKey(TIMING_DATA)) {
            throw new IllegalArgumentException(
                    "Missing required configuration field: "
                            + storageField + "." + TIMING_DATA);
        }

        String timingDataField = storageField + "." + TIMING_DATA;
        Map<?, ?> timingData = requireMapping(storage.get(TIMING_DATA), timingDataField);
        rejectUnknownFields(timingData, timingDataField, PATH);
        String rawPath = requireString(
                timingData.get(PATH),
                timingDataField + "." + PATH).trim();
        if (rawPath.isEmpty()) {
            throw new IllegalArgumentException(
                    timingDataField + "." + PATH + " must not be blank");
        }

        try {
            return Paths.get(rawPath);
        } catch (InvalidPathException ex) {
            throw new IllegalArgumentException(
                    timingDataField + "." + PATH + " is not a valid filesystem path",
                    ex);
        }
    }

    private static LoggingConfig mapLogging(Object rawLogging) {
        if (rawLogging == null) {
            return null;
        }

        Map<?, ?> logging = requireMapping(rawLogging, LOGGING);
        rejectUnknownFields(logging, LOGGING, LEVEL, FILE, LIVE);
        if (!logging.containsKey(LEVEL)) {
            throw new IllegalArgumentException(
                    "Missing required configuration field: " + LOGGING + "." + LEVEL);
        }
        if (!logging.containsKey(FILE)) {
            throw new IllegalArgumentException(
                    "Missing required configuration field: " + LOGGING + "." + FILE);
        }

        String rawLevel = requireString(logging.get(LEVEL), LOGGING + "." + LEVEL);
        LoggingLevel level;
        try {
            level = LoggingLevel.valueOf(rawLevel.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    LOGGING + "." + LEVEL + " must be TRACE, DEBUG, INFO, WARN or ERROR",
                    ex);
        }

        return new LoggingConfig(level, mapLoggingFile(logging.get(FILE)));
    }

    private static LoggingFileConfig mapLoggingFile(Object rawFile) {
        String field = LOGGING + "." + FILE;
        Map<?, ?> values = requireMapping(rawFile, field);
        rejectUnknownFields(values, field, PATH, ROTATE_BYTES, RETAINED_FILES);
        return new LoggingFileConfig(
                requireString(values.get(PATH), field + "." + PATH),
                requirePositiveInteger(values.get(ROTATE_BYTES), field + "." + ROTATE_BYTES),
                requirePositiveInteger(values.get(RETAINED_FILES), field + "." + RETAINED_FILES));
    }

    private static LoggingServerConfig mapLoggingLive(Object rawLogging) {
        if (rawLogging == null) {
            return null;
        }
        Map<?, ?> logging = requireMapping(rawLogging, LOGGING);
        Object rawLive = logging.get(LIVE);
        if (rawLive == null) {
            return null;
        }
        String field = LOGGING + "." + LIVE;
        Map<?, ?> values = endpointMapping(rawLive, field);
        return new LoggingServerConfig(
                requireString(values.get(BIND_ADDRESS), field + "." + BIND_ADDRESS),
                requirePort(values.get(PORT), field + "." + PORT));
    }

    private static Presentation mapPresentation(Object rawPresentation) {
        if (rawPresentation == null) {
            return new Presentation(null, null);
        }

        Map<?, ?> presentation = requireMapping(rawPresentation, PRESENTATION);
        rejectUnknownFields(presentation, PRESENTATION, REMOTE_SHELL, API);

        return new Presentation(
                mapRemoteShell(presentation.get(REMOTE_SHELL)),
                mapApi(presentation.get(API)));
    }

    private static Presentation.RemoteShell mapRemoteShell(Object raw) {
        if (raw == null) {
            return null;
        }
        Map<?, ?> values = endpointMapping(raw, PRESENTATION + "." + REMOTE_SHELL);
        return new Presentation.RemoteShell(
                requireString(values.get(BIND_ADDRESS),
                        PRESENTATION + "." + REMOTE_SHELL + "." + BIND_ADDRESS),
                requirePort(values.get(PORT),
                        PRESENTATION + "." + REMOTE_SHELL + "." + PORT));
    }

    private static Api mapApi(Object raw) {
        if (raw == null) {
            return null;
        }
        String field = PRESENTATION + "." + API;
        Map<?, ?> values = requireMapping(raw, field);
        rejectUnknownFields(values, field, HTTP, WEB_SOCKET);
        return new Api(
                mapApiHttp(values.get(HTTP)),
                mapApiWebSocket(values.get(WEB_SOCKET)));
    }

    private static Api.Http mapApiHttp(Object raw) {
        if (raw == null) {
            return null;
        }
        String field = PRESENTATION + "." + API + "." + HTTP;
        Map<?, ?> values = endpointMapping(raw, field);
        return new Api.Http(
                requireString(values.get(BIND_ADDRESS), field + "." + BIND_ADDRESS),
                requirePort(values.get(PORT), field + "." + PORT));
    }

    private static Api.WebSocket mapApiWebSocket(Object raw) {
        if (raw == null) {
            return null;
        }
        String field = PRESENTATION + "." + API + "." + WEB_SOCKET;
        Map<?, ?> values = endpointMapping(raw, field);
        return new Api.WebSocket(
                requireString(values.get(BIND_ADDRESS), field + "." + BIND_ADDRESS),
                requirePort(values.get(PORT), field + "." + PORT));
    }

    private static Map<?, ?> endpointMapping(Object raw, String field) {
        Map<?, ?> values = requireMapping(raw, field);
        rejectUnknownFields(values, field, BIND_ADDRESS, PORT);
        if (!values.containsKey(BIND_ADDRESS)) {
            throw new IllegalArgumentException(
                    "Missing required configuration field: " + field + "." + BIND_ADDRESS);
        }
        if (!values.containsKey(PORT)) {
            throw new IllegalArgumentException(
                    "Missing required configuration field: " + field + "." + PORT);
        }
        return values;
    }

    private static int requirePositiveInteger(Object value, String field) {
        if (!(value instanceof Integer) || ((Integer) value).intValue() <= 0) {
            throw new IllegalArgumentException(field + " must be a positive YAML integer");
        }
        return ((Integer) value).intValue();
    }

    private static int requirePort(Object value, String field) {
        if (!(value instanceof Integer)) {
            throw new IllegalArgumentException(field + " must be a YAML integer");
        }
        return ((Integer) value).intValue();
    }

    private static Map<?, ?> requireMapping(Object value, String field) {
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException(field + " must be a YAML mapping");
        }
        return (Map<?, ?>) value;
    }

    private static String requireString(Object value, String field) {
        if (!(value instanceof String)) {
            throw new IllegalArgumentException(field + " must be a YAML string");
        }
        return (String) value;
    }

    private static void rejectUnknownFields(
            Map<?, ?> values,
            String field,
            String... allowedFields) {
        for (Object key : values.keySet()) {
            if (!(key instanceof String)
                    || !Arrays.asList(allowedFields).contains((String) key)) {
                throw new IllegalArgumentException(
                        "Unsupported configuration field in " + field + ": " + key);
            }
        }
    }
}

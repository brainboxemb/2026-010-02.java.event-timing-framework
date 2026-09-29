package io.github.brainboxemb.eventtiming.app.bootstrap;

import io.github.brainboxemb.eventtiming.domain.timing.TimingNodeId;
import io.github.brainboxemb.eventtiming.infra.bootstrap.config.ApplicationConfig;
import io.github.brainboxemb.eventtiming.infra.bootstrap.config.PresentationConfig;
import io.github.brainboxemb.eventtiming.infra.bootstrap.config.LoggingConfig;
import io.github.brainboxemb.eventtiming.infra.bootstrap.config.LoggingFileConfig;
import io.github.brainboxemb.eventtiming.infra.bootstrap.config.LoggingLiveConfig;
import io.github.brainboxemb.eventtiming.infra.bootstrap.config.RemoteApiConfig;
import io.github.brainboxemb.eventtiming.infra.bootstrap.config.RemoteApiHttpConfig;
import io.github.brainboxemb.eventtiming.infra.bootstrap.config.RemoteApiWebSocketConfig;
import io.github.brainboxemb.eventtiming.infra.bootstrap.config.RemoteShellConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

/** Maps the default executable YAML format into the framework bootstrap configuration model. */
public final class YamlApplicationConfigLoader {
    private static final String TIMING_NODE_ID = "timingNodeId";
    private static final String PRESENTATION = "presentation";
    private static final String LOGGING = "logging";
    private static final String LEVEL = "level";
    private static final String FILE = "file";
    private static final String PATH = "path";
    private static final String ROTATE_BYTES = "rotateBytes";
    private static final String RETAINED_FILES = "retainedFiles";
    private static final String LIVE = "live";
    private static final String REMOTE_SHELL = "remoteShell";
    private static final String REMOTE_API = "remoteApi";
    private static final String HTTP = "http";
    private static final String WEB_SOCKET = "webSocket";
    private static final String BIND_ADDRESS = "bindAddress";
    private static final String PORT = "port";

    private YamlApplicationConfigLoader() {
    }

    public static ApplicationConfig load(Path path) throws IOException {
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
        rejectUnknownFields(root, "configuration root", TIMING_NODE_ID, PRESENTATION, LOGGING);

        if (!root.containsKey(TIMING_NODE_ID)) {
            throw new IllegalArgumentException(
                    "Missing required configuration field: " + TIMING_NODE_ID);
        }

        TimingNodeId timingNodeId = new TimingNodeId(
                requireString(root.get(TIMING_NODE_ID), TIMING_NODE_ID));
        return new ApplicationConfig(
                timingNodeId,
                mapPresentation(root.get(PRESENTATION)),
                mapLogging(root.get(LOGGING)));
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
        LoggingConfig.Level level;
        try {
            level = LoggingConfig.Level.valueOf(rawLevel.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    LOGGING + "." + LEVEL + " must be TRACE, DEBUG, INFO, WARN or ERROR",
                    ex);
        }

        return new LoggingConfig(
                level,
                mapLoggingFile(logging.get(FILE)),
                mapLoggingLive(logging.get(LIVE)));
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

    private static LoggingLiveConfig mapLoggingLive(Object rawLive) {
        if (rawLive == null) {
            return null;
        }
        String field = LOGGING + "." + LIVE;
        Map<?, ?> values = endpointMapping(rawLive, field);
        return new LoggingLiveConfig(
                requireString(values.get(BIND_ADDRESS), field + "." + BIND_ADDRESS),
                requirePort(values.get(PORT), field + "." + PORT));
    }

    private static PresentationConfig mapPresentation(Object rawPresentation) {
        if (rawPresentation == null) {
            return new PresentationConfig(null, null);
        }

        Map<?, ?> presentation = requireMapping(rawPresentation, PRESENTATION);
        rejectUnknownFields(presentation, PRESENTATION, REMOTE_SHELL, REMOTE_API);

        return new PresentationConfig(
                mapRemoteShell(presentation.get(REMOTE_SHELL)),
                mapRemoteApi(presentation.get(REMOTE_API)));
    }

    private static RemoteShellConfig mapRemoteShell(Object raw) {
        if (raw == null) {
            return null;
        }
        Map<?, ?> values = endpointMapping(raw, PRESENTATION + "." + REMOTE_SHELL);
        return new RemoteShellConfig(
                requireString(values.get(BIND_ADDRESS),
                        PRESENTATION + "." + REMOTE_SHELL + "." + BIND_ADDRESS),
                requirePort(values.get(PORT),
                        PRESENTATION + "." + REMOTE_SHELL + "." + PORT));
    }

    private static RemoteApiConfig mapRemoteApi(Object raw) {
        if (raw == null) {
            return null;
        }
        String field = PRESENTATION + "." + REMOTE_API;
        Map<?, ?> values = requireMapping(raw, field);
        rejectUnknownFields(values, field, HTTP, WEB_SOCKET);
        return new RemoteApiConfig(
                mapRemoteApiHttp(values.get(HTTP)),
                mapRemoteApiWebSocket(values.get(WEB_SOCKET)));
    }

    private static RemoteApiHttpConfig mapRemoteApiHttp(Object raw) {
        if (raw == null) {
            return null;
        }
        String field = PRESENTATION + "." + REMOTE_API + "." + HTTP;
        Map<?, ?> values = endpointMapping(raw, field);
        return new RemoteApiHttpConfig(
                requireString(values.get(BIND_ADDRESS), field + "." + BIND_ADDRESS),
                requirePort(values.get(PORT), field + "." + PORT));
    }

    private static RemoteApiWebSocketConfig mapRemoteApiWebSocket(Object raw) {
        if (raw == null) {
            return null;
        }
        String field = PRESENTATION + "." + REMOTE_API + "." + WEB_SOCKET;
        Map<?, ?> values = endpointMapping(raw, field);
        return new RemoteApiWebSocketConfig(
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

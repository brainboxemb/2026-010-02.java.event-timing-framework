package io.github.brainboxemb.eventtiming.app;

import io.github.brainboxemb.eventtiming.domain.timing.TimingNodeId;

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

/**
 * Loads application configuration from one external YAML file.
 *
 * <p>The loader parses safe generic YAML values and maps them explicitly. Fields without an
 * implemented consumer are rejected.</p>
 */
final class ApplicationConfigLoader {
    private static final String TIMING_NODE_ID = "timingNodeId";
    private static final String PRESENTATION = "presentation";
    private static final String REMOTE_SHELL = "remoteShell";
    private static final String BIND_ADDRESS = "bindAddress";
    private static final String PORT = "port";

    private ApplicationConfigLoader() {
    }

    static ApplicationConfig load(Path path) throws IOException {
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
        rejectUnknownFields(root, "configuration root", TIMING_NODE_ID, PRESENTATION);

        if (!root.containsKey(TIMING_NODE_ID)) {
            throw new IllegalArgumentException(
                    "Missing required configuration field: " + TIMING_NODE_ID);
        }

        TimingNodeId timingNodeId = new TimingNodeId(
                requireString(root.get(TIMING_NODE_ID), TIMING_NODE_ID));
        PresentationConfig presentation = mapPresentation(root.get(PRESENTATION));

        return new ApplicationConfig(timingNodeId, presentation);
    }

    private static PresentationConfig mapPresentation(Object rawPresentation) {
        if (rawPresentation == null) {
            return new PresentationConfig(null);
        }

        Map<?, ?> presentation = requireMapping(rawPresentation, PRESENTATION);
        rejectUnknownFields(presentation, PRESENTATION, REMOTE_SHELL);

        Object rawRemoteShell = presentation.get(REMOTE_SHELL);
        if (rawRemoteShell == null) {
            return new PresentationConfig(null);
        }

        Map<?, ?> remoteShell = requireMapping(rawRemoteShell, PRESENTATION + "." + REMOTE_SHELL);
        rejectUnknownFields(
                remoteShell,
                PRESENTATION + "." + REMOTE_SHELL,
                BIND_ADDRESS,
                PORT);

        if (!remoteShell.containsKey(BIND_ADDRESS)) {
            throw new IllegalArgumentException(
                    "Missing required configuration field: "
                            + PRESENTATION + "." + REMOTE_SHELL + "." + BIND_ADDRESS);
        }
        if (!remoteShell.containsKey(PORT)) {
            throw new IllegalArgumentException(
                    "Missing required configuration field: "
                            + PRESENTATION + "." + REMOTE_SHELL + "." + PORT);
        }

        String bindAddress = requireString(
                remoteShell.get(BIND_ADDRESS),
                PRESENTATION + "." + REMOTE_SHELL + "." + BIND_ADDRESS);
        Object rawPort = remoteShell.get(PORT);
        if (!(rawPort instanceof Integer)) {
            throw new IllegalArgumentException(
                    PRESENTATION + "." + REMOTE_SHELL + "." + PORT
                            + " must be a YAML integer");
        }

        return new PresentationConfig(
                new RemoteShellConfig(bindAddress, ((Integer) rawPort).intValue()));
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

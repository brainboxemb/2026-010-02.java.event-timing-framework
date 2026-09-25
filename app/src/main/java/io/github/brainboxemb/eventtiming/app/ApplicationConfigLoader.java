package io.github.brainboxemb.eventtiming.app;

import io.github.brainboxemb.eventtiming.domain.TimingNodeId;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * Loads the deliberately small A02 configuration from one external YAML file.
 *
 * <p>The loader parses safe generic YAML values and maps them explicitly. It does not use YAML
 * tags/object construction and intentionally rejects fields that do not yet have an implemented
 * consumer.</p>
 */
final class ApplicationConfigLoader {
    private static final String TIMING_NODE_ID = "timingNodeId";

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

        if (!(document instanceof Map)) {
            throw new IllegalArgumentException("Configuration root must be a YAML mapping");
        }

        Map<?, ?> root = (Map<?, ?>) document;
        if (!root.containsKey(TIMING_NODE_ID)) {
            throw new IllegalArgumentException("Missing required configuration field: " + TIMING_NODE_ID);
        }
        if (root.size() != 1) {
            throw new IllegalArgumentException(
                    "A02 configuration supports only the field: " + TIMING_NODE_ID);
        }

        Object rawTimingNodeId = root.get(TIMING_NODE_ID);
        if (!(rawTimingNodeId instanceof String)) {
            throw new IllegalArgumentException(TIMING_NODE_ID + " must be a YAML string");
        }

        return new ApplicationConfig(new TimingNodeId((String) rawTimingNodeId));
    }
}

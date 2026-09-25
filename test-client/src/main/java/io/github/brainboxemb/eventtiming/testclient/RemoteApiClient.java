package io.github.brainboxemb.eventtiming.testclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Independent IF-03 Remote API HTTP client.
 *
 * <p>This project deliberately has no dependency on SI-01 implementation classes.</p>
 */
public final class RemoteApiClient {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final URI endpoint;
    private final HttpClient httpClient;

    public RemoteApiClient(URI endpoint) {
        if (endpoint == null || endpoint.getScheme() == null || endpoint.getHost() == null) {
            throw new IllegalArgumentException("endpoint must be an absolute HTTP URI");
        }
        if (!"http".equalsIgnoreCase(endpoint.getScheme())
                && !"https".equalsIgnoreCase(endpoint.getScheme())) {
            throw new IllegalArgumentException("endpoint scheme must be http or https");
        }
        this.endpoint = endpoint;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public VersionResult getVersion() throws IOException, InterruptedException {
        String rawJson = get("/api/v1/version");
        JsonNode root = JSON.readTree(rawJson);
        return new VersionResult(readBuild(root), rawJson);
    }

    public StatusResult getStatus() throws IOException, InterruptedException {
        return parseStatus(get("/api/v1/status"));
    }

    static StatusResult parseStatus(String rawJson) throws IOException {
        JsonNode root = JSON.readTree(rawJson);
        List<TimingNodeInfo> timingNodes = new ArrayList<>();
        for (JsonNode node : required(root, "timingNodes")) {
            timingNodes.add(new TimingNodeInfo(
                    requiredText(node, "timingNodeId"),
                    requiredText(node, "lifecycle")));
        }
        return new StatusResult(
                readBuild(required(root, "build")),
                List.copyOf(timingNodes),
                rawJson);
    }

    private String get(String path) throws IOException, InterruptedException {
        URI uri = endpoint.resolve(path);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(5))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(
                    "HTTP " + response.statusCode() + " from " + uri + ": " + response.body());
        }
        return response.body();
    }

    private static BuildInfo readBuild(JsonNode root) {
        return new BuildInfo(
                requiredText(root, "application"),
                requiredText(root, "version"),
                requiredText(root, "revision"),
                requiredText(root, "sourceRef"),
                requiredText(root, "buildOrigin"),
                required(root, "dirty").asBoolean(),
                requiredText(root, "apiVersion"));
    }

    static JsonNode required(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("Missing IF-03 field: " + field);
        }
        return value;
    }

    static String requiredText(JsonNode root, String field) {
        JsonNode value = required(root, field);
        if (!value.isTextual()) {
            throw new IllegalArgumentException("IF-03 field is not text: " + field);
        }
        return value.asText();
    }

    public record BuildInfo(
            String application,
            String version,
            String revision,
            String sourceRef,
            String buildOrigin,
            boolean dirty,
            String apiVersion) {
    }

    public record VersionResult(BuildInfo build, String rawJson) {
    }

    public record TimingNodeInfo(String timingNodeId, String lifecycle) {
    }

    public record StatusResult(
            BuildInfo build,
            List<TimingNodeInfo> timingNodes,
            String rawJson) {
    }
}

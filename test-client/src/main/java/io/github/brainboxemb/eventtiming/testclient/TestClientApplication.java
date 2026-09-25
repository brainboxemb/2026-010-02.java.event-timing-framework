package io.github.brainboxemb.eventtiming.testclient;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class TestClientApplication extends Application {
    private final ExecutorService requests = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "event-timing-test-client-http");
        thread.setDaemon(true);
        return thread;
    });

    private final TextField endpoint = new TextField("http://127.0.0.1:8081");
    private final Button getVersion = new Button("Get Version");
    private final Button getStatus = new Button("Get Status");
    private final Label feedback = new Label("Ready");

    private final Label application = valueLabel();
    private final Label version = valueLabel();
    private final Label revision = valueLabel();
    private final Label sourceRef = valueLabel();
    private final Label buildOrigin = valueLabel();
    private final Label sourceState = valueLabel();
    private final Label apiVersion = valueLabel();

    private final Label applicationState = valueLabel();
    private final Label startedAt = valueLabel();
    private final Label timingNodeId = valueLabel();
    private final Label timingNodeLifecycle = valueLabel();

    private final TextArea rawJson = new TextArea();

    @Override
    public void start(Stage stage) {
        endpoint.setPrefColumnCount(40);
        HBox.setHgrow(endpoint, Priority.ALWAYS);

        HBox controls = new HBox(8,
                new Label("SI-01 endpoint"),
                endpoint,
                getVersion,
                getStatus);
        controls.setPadding(new Insets(12));

        getVersion.setOnAction(event -> loadVersion());
        getStatus.setOnAction(event -> loadStatus());

        TitledPane versionPane = new TitledPane("Build / version", versionGrid());
        versionPane.setCollapsible(false);
        TitledPane statusPane = new TitledPane("Status", statusGrid());
        statusPane.setCollapsible(false);

        rawJson.setEditable(false);
        rawJson.setWrapText(false);
        rawJson.setPrefRowCount(16);
        TitledPane rawPane = new TitledPane("Raw JSON", rawJson);
        rawPane.setCollapsible(false);
        VBox.setVgrow(rawPane, Priority.ALWAYS);

        VBox center = new VBox(10, versionPane, statusPane, rawPane);
        center.setPadding(new Insets(0, 12, 12, 12));
        VBox.setVgrow(rawPane, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setTop(controls);
        root.setCenter(center);
        root.setBottom(feedback);
        BorderPane.setMargin(feedback, new Insets(0, 12, 12, 12));

        stage.setTitle("Event Timing Test Client");
        stage.setScene(new Scene(root, 900, 700));
        stage.show();
    }

    private GridPane versionGrid() {
        GridPane grid = grid();
        addRow(grid, 0, "Application", application);
        addRow(grid, 1, "Version", version);
        addRow(grid, 2, "Revision", revision);
        addRow(grid, 3, "Source ref", sourceRef);
        addRow(grid, 4, "Build origin", buildOrigin);
        addRow(grid, 5, "Source state", sourceState);
        addRow(grid, 6, "API version", apiVersion);
        return grid;
    }

    private GridPane statusGrid() {
        GridPane grid = grid();
        addRow(grid, 0, "Application state", applicationState);
        addRow(grid, 1, "Started at", startedAt);
        addRow(grid, 2, "Timing node", timingNodeId);
        addRow(grid, 3, "Lifecycle", timingNodeLifecycle);
        return grid;
    }

    private void loadVersion() {
        runRequest(
                () -> client().getVersion(),
                result -> {
                    showBuild(result.build());
                    rawJson.setText(result.rawJson());
                });
    }

    private void loadStatus() {
        runRequest(
                () -> client().getStatus(),
                result -> {
                    showBuild(result.build());
                    applicationState.setText(result.applicationState());
                    startedAt.setText(result.startedAt().toString());
                    if (result.timingNodes().isEmpty()) {
                        timingNodeId.setText("-");
                        timingNodeLifecycle.setText("-");
                    } else {
                        var node = result.timingNodes().get(0);
                        timingNodeId.setText(node.timingNodeId());
                        timingNodeLifecycle.setText(node.lifecycle());
                    }
                    rawJson.setText(result.rawJson());
                });
    }

    private ApplicationControlClient client() {
        return new ApplicationControlClient(URI.create(endpoint.getText().trim()));
    }

    private void showBuild(ApplicationControlClient.BuildInfo build) {
        application.setText(build.application());
        version.setText(build.version());
        revision.setText(build.revision());
        sourceRef.setText(build.sourceRef());
        buildOrigin.setText(build.buildOrigin());
        sourceState.setText(build.dirty() ? "modified" : "clean");
        apiVersion.setText(build.apiVersion());
    }

    private <T> void runRequest(CheckedSupplier<T> request, java.util.function.Consumer<T> success) {
        setBusy(true);
        feedback.setText("Requesting...");
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return request.get();
                    } catch (Exception ex) {
                        throw new CompletionException(ex);
                    }
                }, requests)
                .whenComplete((result, error) -> Platform.runLater(() -> {
                    setBusy(false);
                    if (error != null) {
                        Throwable cause = error.getCause() == null ? error : error.getCause();
                        feedback.setText("Error: " + cause.getMessage());
                    } else {
                        success.accept(result);
                        feedback.setText("OK");
                    }
                }));
    }

    private void setBusy(boolean busy) {
        getVersion.setDisable(busy);
        getStatus.setDisable(busy);
        endpoint.setDisable(busy);
    }

    private static GridPane grid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(6);
        grid.setPadding(new Insets(10));
        return grid;
    }

    private static void addRow(GridPane grid, int row, String name, Label value) {
        grid.add(new Label(name), 0, row);
        grid.add(value, 1, row);
    }

    private static Label valueLabel() {
        Label label = new Label("-");
        label.setWrapText(true);
        return label;
    }

    @Override
    public void stop() {
        requests.shutdownNow();
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }

    public static void main(String[] args) {
        launch(args);
    }
}

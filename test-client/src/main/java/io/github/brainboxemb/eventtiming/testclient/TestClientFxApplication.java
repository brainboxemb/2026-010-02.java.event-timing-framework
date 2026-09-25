package io.github.brainboxemb.eventtiming.testclient;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
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

public final class TestClientFxApplication extends Application {
    private final TestClientBuildIdentity clientBuild = TestClientBuildIdentity.embedded();
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

    private final Label timingNodeId = valueLabel();
    private final Label timingNodeLifecycle = valueLabel();

    private final TextArea rawJson = new TextArea();

    private final RemoteShellClient shellClient = new RemoteShellClient();
    private final TextField shellHost = new TextField("127.0.0.1");
    private final TextField shellPort = new TextField("8023");
    private final Button shellConnect = new Button("Connect");
    private final Button shellDisconnect = new Button("Disconnect");
    private final TextArea terminal = new TextArea();
    private final TextField terminalInput = new TextField();
    private final Button terminalSend = new Button("Send");
    private final Label terminalStatus = new Label("Disconnected");

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

        VBox statusContent = new VBox(10, versionPane, statusPane, rawPane);
        statusContent.setPadding(new Insets(0, 12, 12, 12));
        VBox.setVgrow(rawPane, Priority.ALWAYS);

        Tab statusTab = new Tab("Status", statusContent);
        statusTab.setClosable(false);
        Tab terminalTab = new Tab("Terminal", terminalPane());
        terminalTab.setClosable(false);
        TabPane tabs = new TabPane(statusTab, terminalTab);

        MenuItem about = new MenuItem("About");
        about.setOnAction(event -> showAbout(stage));
        Menu help = new Menu("Help");
        help.getItems().add(about);
        MenuBar menuBar = new MenuBar(help);

        VBox top = new VBox(menuBar, controls);

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(tabs);
        root.setBottom(feedback);
        BorderPane.setMargin(feedback, new Insets(0, 12, 12, 12));

        stage.setTitle(clientBuild.application() + " — " + clientBuild.version());
        stage.setScene(new Scene(root, 900, 700));
        stage.show();
    }

    private void showAbout(Stage owner) {
        Alert about = new Alert(Alert.AlertType.INFORMATION);
        about.initOwner(owner);
        about.setTitle("About " + clientBuild.application());
        about.setHeaderText(clientBuild.application() + " — " + clientBuild.version());
        about.setContentText(
                "Version      : " + clientBuild.version() + System.lineSeparator()
                        + "Revision     : " + clientBuild.revision() + System.lineSeparator()
                        + "Source ref   : " + clientBuild.sourceRef() + System.lineSeparator()
                        + "Build origin : " + clientBuild.buildOrigin() + System.lineSeparator()
                        + "Source state : " + (clientBuild.dirty() ? "modified" : "clean"));
        about.showAndWait();
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
        addRow(grid, 0, "Timing node", timingNodeId);
        addRow(grid, 1, "Lifecycle", timingNodeLifecycle);
        return grid;
    }

    private VBox terminalPane() {
        shellHost.setPrefColumnCount(18);
        shellPort.setPrefColumnCount(6);
        shellDisconnect.setDisable(true);

        HBox connection = new HBox(8,
                new Label("Host"),
                shellHost,
                new Label("Port"),
                shellPort,
                shellConnect,
                shellDisconnect,
                terminalStatus);

        terminal.setEditable(false);
        terminal.setWrapText(false);
        terminal.setStyle(
                "-fx-control-inner-background: black;"
                        + "-fx-text-fill: #e8e8e8;"
                        + "-fx-font-family: 'Consolas';"
                        + "-fx-font-size: 13px;");
        VBox.setVgrow(terminal, Priority.ALWAYS);

        terminalInput.setPromptText("command");
        terminalInput.setDisable(true);
        terminalSend.setDisable(true);
        HBox.setHgrow(terminalInput, Priority.ALWAYS);
        HBox input = new HBox(8, terminalInput, terminalSend);

        shellConnect.setOnAction(event -> connectShell());
        shellDisconnect.setOnAction(event -> shellClient.disconnect());
        terminalSend.setOnAction(event -> sendShellCommand());
        terminalInput.setOnAction(event -> sendShellCommand());

        VBox pane = new VBox(8, connection, terminal, input);
        pane.setPadding(new Insets(12));
        return pane;
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

    private void connectShell() {
        String host = shellHost.getText().trim();
        int port;
        try {
            port = Integer.parseInt(shellPort.getText().trim());
        } catch (NumberFormatException ex) {
            terminalStatus.setText("Invalid port");
            return;
        }

        shellConnect.setDisable(true);
        shellHost.setDisable(true);
        shellPort.setDisable(true);
        terminalStatus.setText("Connecting...");

        CompletableFuture
                .runAsync(() -> {
                    try {
                        shellClient.connect(host, port, new RemoteShellClient.Listener() {
                            @Override
                            public void onText(String text) {
                                Platform.runLater(() -> {
                                    terminal.appendText(text);
                                    terminal.positionCaret(terminal.getLength());
                                });
                            }

                            @Override
                            public void onDisconnected() {
                                Platform.runLater(() -> setShellConnected(false, "Disconnected"));
                            }

                            @Override
                            public void onError(String message) {
                                Platform.runLater(() -> terminalStatus.setText("Error: " + message));
                            }
                        });
                    } catch (Exception ex) {
                        throw new CompletionException(ex);
                    }
                }, requests)
                .whenComplete((ignored, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        Throwable cause = error.getCause() == null ? error : error.getCause();
                        setShellConnected(false, "Error: " + cause.getMessage());
                    } else {
                        setShellConnected(true, "Connected");
                        terminalInput.requestFocus();
                    }
                }));
    }

    private void sendShellCommand() {
        String command = terminalInput.getText();
        if (command == null || command.trim().isEmpty()) {
            return;
        }
        try {
            terminal.appendText(command + System.lineSeparator());
            terminal.positionCaret(terminal.getLength());
            shellClient.send(command);
            terminalInput.clear();
        } catch (Exception ex) {
            terminalStatus.setText("Error: " + ex.getMessage());
        }
    }

    private void setShellConnected(boolean connected, String status) {
        shellConnect.setDisable(connected);
        shellDisconnect.setDisable(!connected);
        shellHost.setDisable(connected);
        shellPort.setDisable(connected);
        terminalInput.setDisable(!connected);
        terminalSend.setDisable(!connected);
        terminalStatus.setText(status);
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
        shellClient.close();
        requests.shutdownNow();
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }

}

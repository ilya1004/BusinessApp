package oll.businessdesktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class ProcessDesignerController {

    @FXML private WebView webView;
    @FXML private ToolBar toolBar;
    @FXML private Label diagramNameLabel;
    @FXML private Label lastSavedLabel;
    @FXML private VBox propertiesContainer;
    @FXML private Label noSelectionLabel;

    private WebEngine engine;
    private String currentDiagramName;
    private JavaBridge bridge;
    private static final Path DIAGRAMS_DIR = Paths.get("saved-diagrams");
    private static final Path LOG_DIR = Paths.get("js-logs");
    private static final Path LOG_FILE = LOG_DIR.resolve("console.log");
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @FXML
    public void initialize() {
        try {
            Files.createDirectories(DIAGRAMS_DIR);
        } catch (IOException e) {
            e.printStackTrace();
        }

        engine = webView.getEngine();

        try {
            bridge = new JavaBridge(LOG_FILE, this);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Could not initialize: " + e.getMessage());
            return;
        }

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaBridge", bridge);
                try {
                    engine.executeScript("if (typeof window.__enableConsoleBridge === 'function') window.__enableConsoleBridge();");
                } catch (Exception e) {
                    System.err.println("Could not activate console bridge: " + e.getMessage());
                }
            } else if (newState == Worker.State.FAILED) {
                System.err.println("Page load error: " + engine.getLoadWorker().getException());
            }
        });

        engine.setOnError(event -> System.out.println("JS ERROR: " + event.getMessage()));

        engine.getLoadWorker().exceptionProperty().addListener((obs, o, e) -> {
            if (e != null) {
                e.printStackTrace();
            }
        });



        engine.load(Objects.requireNonNull(getClass().getResource("/bpmn-editor.html")).toExternalForm());
        updateLabels();
    }

    @FXML
    private void onNewDiagram() {
        TextInputDialog dialog = new TextInputDialog("Diagram_" + LocalDateTime.now().format(TIMESTAMP_FMT));
        dialog.setTitle("New Diagram");
        dialog.setHeaderText("Enter diagram name");
        dialog.setContentText("Name:");

        dialog.showAndWait().ifPresent(name -> {
            currentDiagramName = name.isBlank()
                    ? LocalDateTime.now().format(TIMESTAMP_FMT)
                    : name.trim();
            lastSavedLabel.setText("");
            engine.reload();
            updateLabels();
        });
    }

    @FXML
    private void onOpenFile() {
        Stage stage = (Stage) webView.getScene().getWindow();
        FileChooser chooser = new FileChooser();
        chooser.setInitialDirectory(DIAGRAMS_DIR.toFile());
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("BPMN Files", "*.bpmn", "*.xml"));
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            try {
                String xml = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                currentDiagramName = stripExtension(file.getName());
                safeExecScript("importXML(`" + escapeForJs(xml) + "`);");
                updateLabels();
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Could not open file: " + ex.getMessage());
            }
        }
    }

    @FXML
    private void onSaveXML() {
        try {
            engine.executeScript("window.saveXML()");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Could not trigger save: " + e.getMessage());
        }
    }

    @FXML
    private void onExportSVG() {
        safeExecScript("exportSVG();");
    }

    void handleXMLSaved(String xml) {
        String fileName = (currentDiagramName != null ? currentDiagramName : LocalDateTime.now().format(TIMESTAMP_FMT)) + ".bpmn";
        Path target = DIAGRAMS_DIR.resolve(fileName);
        try {
            Files.writeString(target, xml, StandardCharsets.UTF_8);
            currentDiagramName = stripExtension(fileName);
            updateLabels();
            lastSavedLabel.setText("Last saved: " + LocalDateTime.now().format(DISPLAY_FMT));
        } catch (IOException ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Could not save diagram: " + ex.getMessage());
        }
    }

    void handleSVGExported(String svg) {
        String fileName = (currentDiagramName != null ? currentDiagramName : LocalDateTime.now().format(TIMESTAMP_FMT)) + ".svg";
        Path target = DIAGRAMS_DIR.resolve(fileName);
        try {
            Files.writeString(target, svg, StandardCharsets.UTF_8);
            Platform.runLater(() -> lastSavedLabel.setText("SVG exported: " + fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void handleElementCreated(String json) {
        System.out.println("[ELEMENT] " + json);
    }

    void handleElementSelected(String json) {
        Platform.runLater(() -> updatePropertiesPanel(json));
    }

    private void updatePropertiesPanel(String json) {
        try {
            JsonNode node = MAPPER.readTree(json);
            propertiesContainer.getChildren().clear();
            noSelectionLabel.setVisible(false);

            addPropertyRow("ID", node.path("id").asText());
            addPropertyRow("Type", node.path("type").asText());
            addPropertyRow("Name", node.path("name").asText("-"));
            addPropertyRow("X", node.path("x").asText("0"));
            addPropertyRow("Y", node.path("y").asText("0"));
            addPropertyRow("Width", node.path("width").asText("0"));
            addPropertyRow("Height", node.path("height").asText("0"));
        } catch (Exception e) {
            System.err.println("Failed to parse element JSON: " + e.getMessage());
        }
    }

    private void addPropertyRow(String label, String value) {
        HBox row = new HBox();
        row.setSpacing(8);
        Label keyLabel = new Label(label + ":");
        keyLabel.setStyle("-fx-font-weight: bold; -fx-min-width: 70;");
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: -color-fg-muted;");
        row.getChildren().addAll(keyLabel, valueLabel);
        propertiesContainer.getChildren().add(row);
    }

    private void updateLabels() {
        if (currentDiagramName != null) {
            diagramNameLabel.setText(currentDiagramName);
        } else {
            diagramNameLabel.setText("Untitled");
        }
    }

    private void safeExecScript(String script) {
        try {
            engine.executeScript(script);
        } catch (Exception e) {
            System.err.println("JS Error: " + e.getMessage());
        }
    }

    private String escapeForJs(String str) {
        return str.replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("${", "\\${")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private void showAlert(Alert.AlertType type, String message) {
        Platform.runLater(() -> new Alert(type, message).show());
    }

    public static class JavaBridge {
        private final BufferedWriter logWriter;
        private final ProcessDesignerController controller;
        private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

        public JavaBridge(Path logFile, ProcessDesignerController controller) throws IOException {
            this.controller = controller;
            Files.createDirectories(logFile.getParent());
            Path absolutePath = logFile.toAbsolutePath().normalize();
            logWriter = Files.newBufferedWriter(absolutePath, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
            writeLog("SYSTEM", "Logger initialized -> " + absolutePath);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try { logWriter.flush(); logWriter.close(); } catch (IOException ignored) {}
            }));
        }

        public void log(String level, String message) {
            writeLog(level, message);
        }

        private synchronized void writeLog(String level, String message) {
            String line = String.format("[%s] [%-6s] %s%n", LocalDateTime.now().format(dtf), level, message);
            try {
                logWriter.write(line);
                logWriter.flush();
            } catch (IOException e) {
                System.err.println("Log write error: " + e.getMessage());
            }
        }

        public void onXMLSaved(String xml) {
            writeLog("JAVA", "XML saved (" + xml.length() + " chars)");
            controller.handleXMLSaved(xml);
        }

        public void onSVGExported(String svg) {
            writeLog("JAVA", "SVG exported (" + svg.length() + " chars)");
            controller.handleSVGExported(svg);
        }

        public void onElementCreated(String json) {
            System.out.println(json);
            controller.handleElementCreated(json);
        }

        public void onElementSelected(String json) {
            controller.handleElementSelected(json);
        }
    }
}

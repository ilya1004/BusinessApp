package oll.businessdesktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import oll.businessdesktop.ApiService;
import oll.businessdesktop.model.ProcessModel;
import oll.businessdesktop.model.TaskDefinition;
import oll.businessdesktop.model.User;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    private ProcessModel currentProcessModel;
    private final Map<String, TaskDefinition> taskDefinitions = new ConcurrentHashMap<>();
    private String selectedElementId;
    private TextField currentNameField;
    private TextField currentDurationField;
    private TextField currentCostField;
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
            currentProcessModel = null;
            taskDefinitions.clear();
            selectedElementId = null;
            lastSavedLabel.setText("");
            engine.reload();
            updateLabels();
            clearPropertiesPanel();
        });
    }

    @FXML
    private void onOpenFromDb() {
        new Thread(() -> {
            try {
                java.util.List<ProcessModel> models = ApiService.getAllProcessModels();
                if (models == null || models.isEmpty()) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Open from DB");
                        alert.setHeaderText(null);
                        alert.setContentText("No process models found in database.");
                        alert.showAndWait();
                    });
                    return;
                }

                Platform.runLater(() -> {
                    ChoiceDialog<String> dialog = new ChoiceDialog<>(models.get(0).name(), models.stream().map(ProcessModel::name).toList());
                    dialog.setTitle("Open from DB");
                    dialog.setHeaderText("Select a diagram to open");
                    dialog.setContentText("Diagram:");

                    dialog.showAndWait().ifPresent(selectedName -> {
                        ProcessModel selected = models.stream().filter(m -> m.name().equals(selectedName)).findFirst().orElse(null);
                        if (selected == null) return;

                        currentDiagramName = selected.name();
                        currentProcessModel = selected;
                        taskDefinitions.clear();
                        selectedElementId = null;
                        clearPropertiesPanel();

                        if (selected.taskDefinitions() != null) {
                            for (TaskDefinition td : selected.taskDefinitions()) {
                                taskDefinitions.put(td.bpmnElementId(), td);
                            }
                        }

                        Path bpmnFile = DIAGRAMS_DIR.resolve(currentDiagramName + ".bpmn");
                        String xml;
                        if (Files.exists(bpmnFile)) {
                            try {
                                xml = Files.readString(bpmnFile, StandardCharsets.UTF_8);
                                lastSavedLabel.setText("Loaded from local file");
                            } catch (IOException e) {
                                xml = selected.bpmnXml();
                                try {
                                    Files.writeString(bpmnFile, xml, StandardCharsets.UTF_8);
                                    lastSavedLabel.setText("Restored from DB to local file");
                                } catch (IOException ex) {
                                    lastSavedLabel.setText("Using BPMN from DB (could not save file)");
                                }
                            }
                        } else {
                            xml = selected.bpmnXml();
                            try {
                                Files.createDirectories(DIAGRAMS_DIR);
                                Files.writeString(bpmnFile, xml, StandardCharsets.UTF_8);
                                lastSavedLabel.setText("Created from DB and saved locally");
                            } catch (IOException e) {
                                lastSavedLabel.setText("Using BPMN from DB (could not save file)");
                            }
                        }

                        if (xml != null && !xml.isBlank()) {
                            safeExecScript("importXML(`" + escapeForJs(xml) + "`);");
                        }

                        Platform.runLater(() -> {
                            updateLabels();
                        });
                    });
                });
            } catch (Exception e) {
                System.err.println("[DB] Failed to load process models: " + e.getMessage());
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Open from DB");
                    alert.setHeaderText(null);
                    alert.setContentText("Failed to load diagrams: " + e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
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
                currentProcessModel = null;
                taskDefinitions.clear();
                selectedElementId = null;
                safeExecScript("importXML(`" + escapeForJs(xml) + "`);");
                updateLabels();
                clearPropertiesPanel();

                new Thread(() -> {
                    try {
                        ProcessModel existing = ApiService.findProcessModelByName(currentDiagramName);
                        Platform.runLater(() -> {
                            if (existing != null) {
                                currentProcessModel = existing;
                                if (existing.taskDefinitions() != null) {
                                    for (TaskDefinition td : existing.taskDefinitions()) {
                                        taskDefinitions.put(td.bpmnElementId(), td);
                                    }
                                }
                                lastSavedLabel.setText("Linked to DB model id=" + existing.id());
                            } else {
                                lastSavedLabel.setText("New diagram (not in DB)");
                            }
                        });
                    } catch (Exception e) {
                        System.err.println("[DB] Lookup by name failed: " + e.getMessage());
                    }
                }).start();
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
        List<TaskDefinition> taskList = new ArrayList<>(taskDefinitions.values());
        System.out.println("[SAVE] Preparing to save " + taskList.size() + " TaskDefinitions");
        for (TaskDefinition td : taskList) {
            System.out.println("[SAVE]   - id=" + td.id() + ", elementId=" + td.bpmnElementId() + ", name=" + td.name());
        }
        
        new Thread(() -> {
            try {
                Long authorId = null;
                try {
                    User currentUser = ApiService.getCurrentUser();
                    if (currentUser != null) {
                        authorId = currentUser.id();
                    }
                } catch (Exception e) {
                    System.out.println("[DB] Could not get current user: " + e.getMessage());
                }

                ProcessModel result;
                if (currentProcessModel == null) {
                    ProcessModel existing = ApiService.findProcessModelByName(currentDiagramName);
                    if (existing != null) {
                        result = ApiService.updateProcessModel(existing.id(), currentDiagramName, xml, existing.authorId(), taskList);
                        System.out.println("[DB] Updated ProcessModel id=" + result.id());
                    } else {
                        result = ApiService.createProcessModel(currentDiagramName, xml, authorId, taskList);
                        System.out.println("[DB] Created ProcessModel id=" + result.id() + " with " + (result.taskDefinitions() != null ? result.taskDefinitions().size() : 0) + " tasks");
                    }
                } else {
                    result = ApiService.updateProcessModel(currentProcessModel.id(), currentDiagramName, xml, currentProcessModel.authorId(), taskList);
                    System.out.println("[DB] Updated ProcessModel id=" + result.id());
                }

                if (result != null && result.taskDefinitions() != null) {
                    Platform.runLater(() -> {
                        taskDefinitions.clear();
                        for (TaskDefinition td : result.taskDefinitions()) {
                            taskDefinitions.put(td.bpmnElementId(), td);
                        }
                        System.out.println("[DB] Synced " + taskDefinitions.size() + " TaskDefinitions locally");
                    });
                }

                Platform.runLater(() -> {
                    String fileName = currentDiagramName + ".bpmn";
                    Path target = DIAGRAMS_DIR.resolve(fileName);
                    try {
                        Files.writeString(target, xml, StandardCharsets.UTF_8);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    updateLabels();
                    lastSavedLabel.setText("Last saved: " + LocalDateTime.now().format(DISPLAY_FMT));
                });
            } catch (Exception e) {
                System.err.println("[DB] Save failed: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Could not save to server: " + e.getMessage()));
            }
        }).start();
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
        System.out.println("[ELEMENT CREATED] " + json);
        Platform.runLater(() -> {
            try {
                JsonNode node = MAPPER.readTree(json);
                String elementId = node.path("id").asText();
                String elementType = node.path("type").asText();
                String name = node.path("name").asText(elementId);

                if (elementType.startsWith("bpmn:SequenceFlow") || elementType.startsWith("bpmn:MessageFlow") || elementType.startsWith("bpmn:Association")) {
                    return;
                }

                if (!taskDefinitions.containsKey(elementId)) {
                    TaskDefinition td = new TaskDefinition(elementId, name);
                    taskDefinitions.put(elementId, td);
                    System.out.println("[TD] Created for " + elementId + " (" + elementType + ")");
                }
            } catch (Exception e) {
                System.err.println("[TD] Failed to create: " + e.getMessage());
            }
        });
    }

    void handleElementSelected(String json) {
        Platform.runLater(() -> updatePropertiesPanel(json));
    }

    private void clearPropertiesPanel() {
        selectedElementId = null;
        currentNameField = null;
        currentDurationField = null;
        currentCostField = null;
        propertiesContainer.getChildren().clear();
        noSelectionLabel.setVisible(true);
    }

    private void updatePropertiesPanel(String json) {
        try {
            JsonNode node = MAPPER.readTree(json);
            selectedElementId = node.path("id").asText();
            propertiesContainer.getChildren().clear();
            noSelectionLabel.setVisible(false);

            addPropertyRow("ID", selectedElementId);
            addPropertyRow("Type", node.path("type").asText());

            TaskDefinition td = taskDefinitions.get(selectedElementId);
            if (td != null) {
                Separator sep = new Separator();
                VBox.setMargin(sep, new javafx.geometry.Insets(5, 0, 5, 0));
                propertiesContainer.getChildren().add(sep);

                Label header = new Label("Task Definition");
                header.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
                propertiesContainer.getChildren().add(header);

                currentNameField = new TextField(td.name());
                currentNameField.setPromptText("Name");
                addEditableRow("Name", currentNameField);
                currentNameField.textProperty().addListener((obs, old, newVal) -> {
                    TaskDefinition current = taskDefinitions.get(selectedElementId);
                    if (current != null) {
                        TaskDefinition updated = new TaskDefinition(current.id(), current.bpmnElementId(), newVal, current.defaultDuration(), current.expectedCost(), current.kpiWeight());
                        taskDefinitions.put(selectedElementId, updated);
                    }
                    safeExecScript("updateElementName('" + selectedElementId + "', '" + newVal.replace("'", "\\'") + "');");
                });

                currentDurationField = new TextField(String.valueOf(td.defaultDuration()));
                currentDurationField.setPromptText("Duration");
                currentDurationField.setTextFormatter(new TextFormatter<>(change -> {
                    if (change.getControlNewText().matches("\\d*")) {
                        return change;
                    }
                    return null;
                }));
                addEditableRow("Duration", currentDurationField);
                currentDurationField.textProperty().addListener((obs, old, newVal) -> {
                    try {
                        int duration = newVal.isBlank() ? 0 : Integer.parseInt(newVal);
                        TaskDefinition current = taskDefinitions.get(selectedElementId);
                        if (current != null) {
                            TaskDefinition updated = new TaskDefinition(current.id(), current.bpmnElementId(), current.name(), duration, current.expectedCost(), current.kpiWeight());
                            taskDefinitions.put(selectedElementId, updated);
                        }
                    } catch (NumberFormatException e) {}
                });

                currentCostField = new TextField(td.expectedCost().toPlainString());
                currentCostField.setPromptText("Cost");
                currentCostField.setTextFormatter(new TextFormatter<>(change -> {
                    if (change.getControlNewText().matches("\\d*(\\.\\d{0,2})?")) {
                        return change;
                    }
                    return null;
                }));
                addEditableRow("Cost ($)", currentCostField);
                currentCostField.textProperty().addListener((obs, old, newVal) -> {
                    try {
                        BigDecimal cost = newVal.isBlank() ? BigDecimal.ZERO : new BigDecimal(newVal);
                        TaskDefinition current = taskDefinitions.get(selectedElementId);
                        if (current != null) {
                            TaskDefinition updated = new TaskDefinition(current.id(), current.bpmnElementId(), current.name(), current.defaultDuration(), cost, current.kpiWeight());
                            taskDefinitions.put(selectedElementId, updated);
                        }
                    } catch (NumberFormatException e) {}
                });
            }
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

    private void addEditableRow(String label, TextField field) {
        HBox row = new HBox();
        row.setSpacing(8);
        Label keyLabel = new Label(label + ":");
        keyLabel.setStyle("-fx-font-weight: bold; -fx-min-width: 70;");
        HBox.setHgrow(field, Priority.ALWAYS);
        row.getChildren().addAll(keyLabel, field);
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
            controller.handleElementCreated(json);
        }

        public void onElementSelected(String json) {
            controller.handleElementSelected(json);
        }
    }
}

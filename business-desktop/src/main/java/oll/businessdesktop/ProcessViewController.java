package oll.businessdesktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import oll.businessdesktop.model.ProcessModel;
import oll.businessdesktop.model.TaskDefinition;
import oll.businessdesktop.model.ProcessInstance;
import oll.businessdesktop.model.KpiModelData;
import oll.businessdesktop.model.KpiWeights;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public class ProcessViewController {

    @FXML private ComboBox<String> diagramSelector;
    @FXML private Button createInstanceBtn;
    @FXML private Label statusLabel;
    @FXML private WebView diagramViewer;
    @FXML private VBox infoContainer;
    @FXML private HBox processInfoRows;
    @FXML private TableView<TaskDefinitionRow> taskTable;
    @FXML private TableColumn<TaskDefinitionRow, String> colElementId;
    @FXML private TableColumn<TaskDefinitionRow, String> colName;
    @FXML private TableColumn<TaskDefinitionRow, Integer> colDuration;
    @FXML private TableColumn<TaskDefinitionRow, BigDecimal> colCost;
    @FXML private TableColumn<TaskDefinitionRow, BigDecimal> colKpiWeight;
    @FXML private Label kpiAvgDurationLabel;
    @FXML private Label kpiDelayRateLabel;
    @FXML private Label kpiRatingLabel;
    @FXML private VBox kpiCardsContainer;
    @FXML private VBox kpiWeightsContainer;
    @FXML private Spinner<Double> w1Spinner;
    @FXML private Spinner<Double> w2Spinner;
    @FXML private Spinner<Double> w3Spinner;
    @FXML private Label weightsSumLabel;

    private WebEngine viewerEngine;
    private boolean viewerReady = false;
    private ProcessModel currentModel;

    private static final Path DIAGRAMS_DIR = Paths.get("saved-diagrams");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @FXML
    public void initialize() {
        try {
            Files.createDirectories(DIAGRAMS_DIR);
        } catch (IOException e) {
            e.printStackTrace();
        }

        colElementId.setCellValueFactory(new PropertyValueFactory<>("elementId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDuration.setCellValueFactory(new PropertyValueFactory<>("duration"));
        colCost.setCellValueFactory(new PropertyValueFactory<>("cost"));
        colKpiWeight.setCellValueFactory(new PropertyValueFactory<>("kpiWeight"));

        SpinnerValueFactory<Double> svf1 = new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 1.0, 0.34, 0.01);
        SpinnerValueFactory<Double> svf2 = new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 1.0, 0.33, 0.01);
        SpinnerValueFactory<Double> svf3 = new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 1.0, 0.33, 0.01);
        w1Spinner.setValueFactory(svf1);
        w2Spinner.setValueFactory(svf2);
        w3Spinner.setValueFactory(svf3);

        ChangeListener<Double> weightListener = (obs, old, newVal) -> updateWeightsSum();
        w1Spinner.valueProperty().addListener(weightListener);
        w2Spinner.valueProperty().addListener(weightListener);
        w3Spinner.valueProperty().addListener(weightListener);

        viewerEngine = diagramViewer.getEngine();
        viewerEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                viewerReady = true;
                if (currentModel != null) {
                    showDiagram(currentModel);
                }
            }
        });

        String viewerPath = Objects.requireNonNull(getClass().getResource("/bpmn-viewer.html")).toExternalForm();
        viewerEngine.load(viewerPath);

        diagramViewer.widthProperty().addListener((obs, oldW, newW) -> {
            if (viewerReady && currentModel != null) {
                viewerEngine.executeScript("zoomToFit(" + newW.doubleValue() + ")");
            }
        });

        loadDiagramList();
    }

    @FXML
    private void onCreateInstance() {
        String selectedName = diagramSelector.getValue();
        if (selectedName == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Start Instance");
            alert.setHeaderText(null);
            alert.setContentText("Please select a diagram first.");
            alert.showAndWait();
            return;
        }

        ProcessModel selectedModel = resolveModelByName(selectedName);
        if (selectedModel == null) {
            statusLabel.setText("Model not found: " + selectedName);
            return;
        }

        TextInputDialog nameDialog = new TextInputDialog("Instance_" + selectedName);
        nameDialog.setTitle("Start Process Instance");
        nameDialog.setHeaderText("Create new instance of: " + selectedName);
        nameDialog.setContentText("Instance name:");

        nameDialog.showAndWait().ifPresent(instanceName -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm");
            confirm.setHeaderText("Create Process Instance");
            confirm.setContentText("Create instance \"" + instanceName + "\" with " +
                    (selectedModel.taskDefinitions() != null ? selectedModel.taskDefinitions().size() : 0) + " tasks?");

            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.OK) {
                    statusLabel.setText("Creating instance...");
                    new Thread(() -> {
                        try {
                            ProcessInstance instance = ApiService.createProcessInstance(selectedModel.id(), instanceName);
                            Platform.runLater(() -> {
                                statusLabel.setText("Created instance id=" + instance.id());
                                MainLayoutController.navigateToInstances(instance.id());
                            });
                        } catch (Exception e) {
                            Platform.runLater(() -> {
                                statusLabel.setText("Failed: " + e.getMessage());
                                Alert alert = new Alert(Alert.AlertType.ERROR);
                                alert.setTitle("Error");
                                alert.setHeaderText(null);
                                alert.setContentText("Failed to create instance: " + e.getMessage());
                                alert.showAndWait();
                            });
                        }
                    }).start();
                }
            });
        });
    }

    private ProcessModel resolveModelByName(String name) {
        try {
            return ApiService.findProcessModelByName(name);
        } catch (Exception e) {
            return null;
        }
    }

    private void loadDiagramList() {
        statusLabel.setText("Loading diagrams...");
        new Thread(() -> {
            try {
                java.util.List<ProcessModel> models = ApiService.getAllProcessModels();
                Platform.runLater(() -> {
                    if (models == null || models.isEmpty()) {
                        diagramSelector.setItems(FXCollections.emptyObservableList());
                        statusLabel.setText("No diagrams in database");
                        return;
                    }
                    diagramSelector.setItems(FXCollections.observableArrayList(
                            models.stream().map(ProcessModel::name).toList()));
                    statusLabel.setText(models.size() + " diagram(s) available");
                    diagramSelector.setOnAction(e -> onDiagramSelected());
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Failed to load diagrams: " + e.getMessage());
                });
            }
        }).start();
    }

    private void onDiagramSelected() {
        String selectedName = diagramSelector.getValue();
        if (selectedName == null) return;

        statusLabel.setText("Loading " + selectedName + "...");
        new Thread(() -> {
            try {
                ProcessModel model = ApiService.findProcessModelByName(selectedName);
                if (model == null) {
                    Platform.runLater(() -> statusLabel.setText("Diagram not found"));
                    return;
                }
                currentModel = model;
                Platform.runLater(() -> showDiagram(model));
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Failed to load: " + e.getMessage()));
            }
        }).start();
    }

    private void showDiagram(ProcessModel model) {
        Path bpmnFile = DIAGRAMS_DIR.resolve(model.name() + ".bpmn");
        String xml;
        if (Files.exists(bpmnFile)) {
            try {
                xml = Files.readString(bpmnFile, StandardCharsets.UTF_8);
                statusLabel.setText("Loaded from local file");
            } catch (IOException e) {
                xml = model.bpmnXml();
                try {
                    Files.writeString(bpmnFile, xml, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
                    statusLabel.setText("Restored from DB to local file");
                } catch (IOException ex) {
                    statusLabel.setText("Using BPMN from DB (could not save file)");
                }
            }
        } else {
            xml = model.bpmnXml();
            try {
                Files.createDirectories(DIAGRAMS_DIR);
                Files.writeString(bpmnFile, xml, StandardCharsets.UTF_8);
                statusLabel.setText("Created from DB and saved locally");
            } catch (IOException e) {
                statusLabel.setText("Using BPMN from DB (could not save file)");
            }
        }

        if (viewerReady) {
            viewerEngine.executeScript("renderDiagram(`" + escapeForJs(xml) + "`)");
        }

        showProcessInfo(model);
        showTaskDefinitions(model);
        loadKpiForModel();
    }

    private void showProcessInfo(ProcessModel model) {
        processInfoRows.getChildren().clear();
        addInfoItem("Name", model.name());
        addInfoItem("Version", String.valueOf(model.version()));
        addInfoItem("Author ID", model.authorId() != null ? model.authorId().toString() : "N/A");
        addInfoItem("Task Count", model.taskDefinitions() != null ? String.valueOf(model.taskDefinitions().size()) : "0");
    }

    private void addInfoItem(String label, String value) {
        VBox item = new VBox();
        item.setSpacing(2);
        Label keyLabel = new Label(label);
        keyLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-fg-muted;");
        Label valLabel = new Label(value);
        valLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        item.getChildren().addAll(keyLabel, valLabel);
        processInfoRows.getChildren().add(item);
    }

    private String escapeForJs(String str) {
        return str.replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("${", "\\${")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static class TaskDefinitionRow {
        private final String elementId;
        private final String name;
        private final Integer duration;
        private final BigDecimal cost;
        private final BigDecimal kpiWeight;

        public TaskDefinitionRow(String elementId, String name, Integer duration, BigDecimal cost, BigDecimal kpiWeight) {
            this.elementId = elementId;
            this.name = name;
            this.duration = duration;
            this.cost = cost;
            this.kpiWeight = kpiWeight;
        }

        public String getElementId() { return elementId; }
        public String getName() { return name; }
        public Integer getDuration() { return duration; }
        public BigDecimal getCost() { return cost; }
        public BigDecimal getKpiWeight() { return kpiWeight != null ? kpiWeight : BigDecimal.ZERO; }
    }

    private void updateWeightsSum() {
        Double w1 = w1Spinner.getValue();
        Double w2 = w2Spinner.getValue();
        Double w3 = w3Spinner.getValue();
        double sum = (w1 != null ? w1 : 0) + (w2 != null ? w2 : 0) + (w3 != null ? w3 : 0);
        weightsSumLabel.setText(String.format("Sum: %.2f", sum));
        weightsSumLabel.setStyle(sum > 1.001 || sum < 0.999 ? "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: -color-danger-emphasis;" : "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: -color-success-emphasis;");
    }

    @FXML
    private void onSaveWeights() {
        Double w1 = w1Spinner.getValue();
        Double w2 = w2Spinner.getValue();
        Double w3 = w3Spinner.getValue();
        if (w1 == null || w2 == null || w3 == null) return;

        double sum = w1 + w2 + w3;
        if (sum > 1.001 || sum < 0.999) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("KPI Weights");
            alert.setHeaderText(null);
            alert.setContentText("Weights must sum to 1.0. Current sum: " + String.format("%.2f", sum));
            alert.showAndWait();
            return;
        }

        Long modelId = currentModel != null ? currentModel.id() : null;
        statusLabel.setText("Saving KPI weights...");
        new Thread(() -> {
            try {
                ApiService.saveKpiWeights(modelId,
                        BigDecimal.valueOf(w1), BigDecimal.valueOf(w2), BigDecimal.valueOf(w3));
                Platform.runLater(() -> {
                    statusLabel.setText("KPI weights saved");
                    loadKpiForModel();
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Failed: " + e.getMessage()));
            }
        }).start();
    }

    private void loadKpiForModel() {
        if (currentModel == null) return;

        new Thread(() -> {
            try {
                KpiModelData kpi = ApiService.getModelKpi(currentModel.id());
                KpiWeights weights = ApiService.getKpiWeights(currentModel.id());

                Platform.runLater(() -> {
                    kpiAvgDurationLabel.setText(kpi.avgDuration() + " min");
                    kpiDelayRateLabel.setText(String.format("%.1f%%", kpi.delayRate() * 100));
                    kpiRatingLabel.setText(String.format("%.3f", kpi.rating()));

                    w1Spinner.getValueFactory().setValue(weights.w1().doubleValue());
                    w2Spinner.getValueFactory().setValue(weights.w2().doubleValue());
                    w3Spinner.getValueFactory().setValue(weights.w3().doubleValue());
                    updateWeightsSum();

                    kpiCardsContainer.setVisible(true);
                    kpiWeightsContainer.setVisible(true);

                    showTaskDefinitions(currentModel, kpi);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    kpiCardsContainer.setVisible(false);
                    kpiWeightsContainer.setVisible(false);
                    statusLabel.setText("KPI data not available: " + e.getMessage());
                });
            }
        }).start();
    }

    private void showTaskDefinitions(ProcessModel model) {
        showTaskDefinitions(model, null);
    }

    private void showTaskDefinitions(ProcessModel model, KpiModelData kpi) {
        java.util.List<TaskDefinitionRow> rows = new java.util.ArrayList<>();
        if (model.taskDefinitions() != null) {
            for (TaskDefinition td : model.taskDefinitions()) {
                BigDecimal kpiW = null;
                if (kpi != null && kpi.tasks() != null) {
                    for (KpiModelData.KpiTaskData kt : kpi.tasks()) {
                        if (kt.elementId().equals(td.bpmnElementId())) {
                            kpiW = kt.kpiWeight();
                            break;
                        }
                    }
                } else {
                    kpiW = td.getKpiWeight();
                }
                rows.add(new TaskDefinitionRow(td.bpmnElementId(), td.name(), td.defaultDuration(), td.expectedCost(), kpiW));
            }
        }
        taskTable.setItems(FXCollections.observableArrayList(rows));
    }
}

package oll.businessdesktop;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import oll.businessdesktop.model.ProcessModel;
import oll.businessdesktop.model.SimulationResponse;
import oll.businessdesktop.model.SimulationResponse.TaskPrediction;

import java.util.List;

public class SimulationController {

    @FXML private ComboBox<String> modelSelector;
    @FXML private Button runBtn;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label statusLabel;
    @FXML private Slider durationSlider;
    @FXML private Label durationLabel;
    @FXML private Slider resourcesSlider;
    @FXML private Label resourcesLabel;
    @FXML private Slider parallelismSlider;
    @FXML private Label parallelismLabel;
    @FXML private VBox chartContainer;
    @FXML private VBox tableContainer;
    @FXML private BarChart<String, Number> comparisonChart;
    @FXML private CategoryAxis chartXAxis;
    @FXML private NumberAxis chartYAxis;
    @FXML private TableView<TaskPredictionRow> predictionTable;
    @FXML private TableColumn<TaskPredictionRow, String> colTaskName;
    @FXML private TableColumn<TaskPredictionRow, Double> colBaseDuration;
    @FXML private TableColumn<TaskPredictionRow, Double> colScenarioDuration;
    @FXML private TableColumn<TaskPredictionRow, Double> colBaseCost;
    @FXML private TableColumn<TaskPredictionRow, Double> colScenarioCost;
    @FXML private TableColumn<TaskPredictionRow, Double> colResourceLoad;

    private List<ProcessModel> allModels;

    @FXML
    public void initialize() {
        durationSlider.valueProperty().addListener((obs, old, val) ->
                durationLabel.setText(String.format("%.1fx", val.doubleValue() / 10.0)));
        resourcesSlider.valueProperty().addListener((obs, old, val) ->
                resourcesLabel.setText(String.valueOf((int) val.doubleValue())));
        parallelismSlider.valueProperty().addListener((obs, old, val) ->
                parallelismLabel.setText(String.valueOf((int) val.doubleValue())));

        modelSelector.setOnAction(e -> runBtn.setDisable(modelSelector.getValue() == null));

        colTaskName.setCellValueFactory(new PropertyValueFactory<>("taskName"));
        colBaseDuration.setCellValueFactory(new PropertyValueFactory<>("baseDuration"));
        colScenarioDuration.setCellValueFactory(new PropertyValueFactory<>("scenarioDuration"));
        colBaseCost.setCellValueFactory(new PropertyValueFactory<>("baseCost"));
        colScenarioCost.setCellValueFactory(new PropertyValueFactory<>("scenarioCost"));
        colResourceLoad.setCellValueFactory(new PropertyValueFactory<>("resourceLoad"));

        // Custom TableCell for Resource Load with color coding
        colResourceLoad.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle(null);
                    return;
                }
                setText(String.format("%.1f%%", item));
                if (item < 70) {
                    setStyle("-fx-background-color: #dcfce7; -fx-padding: 4 8;");
                } else if (item <= 90) {
                    setStyle("-fx-background-color: #fef08a; -fx-padding: 4 8;");
                } else {
                    setStyle("-fx-background-color: #fee2e2; -fx-padding: 4 8;");
                }
            }
        });

        loadModelList();
    }

    private void loadModelList() {
        statusLabel.setText("Loading process models...");
        new Thread(() -> {
            try {
                allModels = ApiService.getAllProcessModels();
                Platform.runLater(() -> {
                    if (allModels == null || allModels.isEmpty()) {
                        modelSelector.setItems(FXCollections.emptyObservableList());
                        statusLabel.setText("No process models found");
                        return;
                    }
                    modelSelector.setItems(FXCollections.observableArrayList(
                            allModels.stream().map(ProcessModel::name).toList()));
                    statusLabel.setText(allModels.size() + " model(s) available");
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        statusLabel.setText("Failed to load models: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void onRunSimulation() {
        String selected = modelSelector.getValue();
        if (selected == null || allModels == null) return;

        ProcessModel selectedModel = null;
        for (ProcessModel m : allModels) {
            if (m.name().equals(selected)) {
                selectedModel = m;
                break;
            }
        }
        if (selectedModel == null) return;

        final ProcessModel targetModel = selectedModel;
        final double durationMultiplier = durationSlider.getValue() / 10.0;
        final int resources = (int) resourcesSlider.getValue();
        final int parallelism = (int) parallelismSlider.getValue();

        progressIndicator.setVisible(true);
        runBtn.setDisable(true);
        statusLabel.setText("Running simulation...");

        Task<SimulationResponse> simTask = new Task<>() {
            @Override
            protected SimulationResponse call() throws Exception {
                return ApiService.runSimulation(targetModel.id(), durationMultiplier, resources, parallelism);
            }
        };

        simTask.setOnSucceeded(e -> {
            progressIndicator.setVisible(false);
            runBtn.setDisable(false);
            SimulationResponse result = simTask.getValue();
            if (result != null) {
                populateChart(result);
                populateTable(result.taskPredictions());
                statusLabel.setText("Simulation complete for " + result.modelName());
            }
        });

        simTask.setOnFailed(e -> {
            progressIndicator.setVisible(false);
            runBtn.setDisable(false);
            statusLabel.setText("Simulation failed: " + simTask.getException().getMessage());
        });

        new Thread(simTask).start();
    }

    private void populateChart(SimulationResponse result) {
        comparisonChart.getData().clear();

        XYChart.Series<String, Number> baseline = new XYChart.Series<>();
        baseline.setName("Baseline");
        XYChart.Series<String, Number> scenario = new XYChart.Series<>();
        scenario.setName("Scenario");

        if (result.cycleTime() != null) {
            baseline.getData().add(new XYChart.Data<>("Cycle Time", result.cycleTime().baseline()));
            scenario.getData().add(new XYChart.Data<>("Cycle Time", result.cycleTime().scenario()));
        }
        if (result.totalCost() != null) {
            baseline.getData().add(new XYChart.Data<>("Total Cost", result.totalCost().baseline()));
            scenario.getData().add(new XYChart.Data<>("Total Cost", result.totalCost().scenario()));
        }
        if (result.resourceLoad() != null) {
            baseline.getData().add(new XYChart.Data<>("Resource Load", result.resourceLoad().baseline()));
            scenario.getData().add(new XYChart.Data<>("Resource Load", result.resourceLoad().scenario()));
        }

        comparisonChart.getData().addAll(baseline, scenario);
        chartContainer.setVisible(true);
    }

    private void populateTable(List<TaskPrediction> predictions) {
        if (predictions == null) {
            predictionTable.setItems(FXCollections.emptyObservableList());
            return;
        }
        List<TaskPredictionRow> rows = predictions.stream()
                .map(p -> new TaskPredictionRow(
                        p.taskName(),
                        p.baseDuration() != null ? p.baseDuration() : 0.0,
                        p.scenarioDuration() != null ? p.scenarioDuration() : 0.0,
                        p.baseCost() != null ? p.baseCost() : 0.0,
                        p.scenarioCost() != null ? p.scenarioCost() : 0.0,
                        p.resourceLoadPercent() != null ? p.resourceLoadPercent() : 0.0
                ))
                .toList();
        predictionTable.setItems(FXCollections.observableArrayList(rows));
        tableContainer.setVisible(true);
    }

    public static class TaskPredictionRow {
        private final String taskName;
        private final Double baseDuration;
        private final Double scenarioDuration;
        private final Double baseCost;
        private final Double scenarioCost;
        private final Double resourceLoad;

        public TaskPredictionRow(String taskName, Double baseDuration, Double scenarioDuration,
                                 Double baseCost, Double scenarioCost, Double resourceLoad) {
            this.taskName = taskName;
            this.baseDuration = baseDuration;
            this.scenarioDuration = scenarioDuration;
            this.baseCost = baseCost;
            this.scenarioCost = scenarioCost;
            this.resourceLoad = resourceLoad;
        }

        public String getTaskName() { return taskName; }
        public Double getBaseDuration() { return baseDuration; }
        public Double getScenarioDuration() { return scenarioDuration; }
        public Double getBaseCost() { return baseCost; }
        public Double getScenarioCost() { return scenarioCost; }
        public Double getResourceLoad() { return resourceLoad; }
    }
}

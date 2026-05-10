package oll.businessdesktop;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import oll.businessdesktop.model.ProcessModel;
import oll.businessdesktop.model.TaskDefinition;
import oll.businessdesktop.model.ProcessInstance;

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
    @FXML private TableColumn<TaskDefinitionRow, Long> colId;
    @FXML private TableColumn<TaskDefinitionRow, String> colName;
    @FXML private TableColumn<TaskDefinitionRow, Integer> colDuration;
    @FXML private TableColumn<TaskDefinitionRow, BigDecimal> colCost;
    @FXML private TableColumn<TaskDefinitionRow, BigDecimal> colKpiWeight;
    @FXML private TableColumn<TaskDefinitionRow, Void> colActions;

    private WebEngine viewerEngine;
    private boolean viewerReady = false;
    private ProcessModel currentModel;

    private static final Path BPMN_DIR = Paths.get("saved-diagrams/bpmn");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @FXML
    public void initialize() {
        try {
                Files.createDirectories(BPMN_DIR);
        } catch (IOException e) {
            e.printStackTrace();
        }

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDuration.setCellValueFactory(new PropertyValueFactory<>("duration"));
        colCost.setCellValueFactory(new PropertyValueFactory<>("cost"));
        colKpiWeight.setCellValueFactory(new PropertyValueFactory<>("kpiWeight"));

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Редактировать");
            {
                editBtn.getStyleClass().add("table-action-button");
                editBtn.setOnAction(e -> {
                    TaskDefinitionRow row = getTableRow().getItem();
                    if (row != null) openEditDialog(row);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    setGraphic(editBtn);
                }
            }
        });

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
            alert.setTitle("Запуск экземпляра");
            alert.setHeaderText(null);
            alert.setContentText("Пожалуйста, выберите диаграмму.");
            alert.showAndWait();
            return;
        }

        ProcessModel selectedModel = resolveModelByName(selectedName);
        if (selectedModel == null) {
            statusLabel.setText("Модель не найдена: " + selectedName);
            return;
        }

        TextInputDialog nameDialog = new TextInputDialog("Instance_" + selectedName);
        nameDialog.setTitle("Запуск экземпляра процесса");
        nameDialog.setHeaderText("Создание экземпляра: " + selectedName);
        nameDialog.setContentText("Имя экземпляра:");

        nameDialog.showAndWait().ifPresent(instanceName -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Подтверждение");
            confirm.setHeaderText("Создание экземпляра процесса");
            confirm.setContentText("Создать экземпляр \"" + instanceName + "\" с " +
                    (selectedModel.taskDefinitions() != null ? selectedModel.taskDefinitions().size() : 0) + " задачами?");

            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.OK) {
                    statusLabel.setText("Создание экземпляра...");
                    new Thread(() -> {
                        try {
                            ProcessInstance instance = ApiService.createProcessInstance(selectedModel.id(), instanceName);
                            Platform.runLater(() -> {
                                statusLabel.setText("Создан экземпляр id=" + instance.id());
                                MainLayoutController.navigateToInstances(instance.id());
                            });
                        } catch (Exception e) {
                            Platform.runLater(() -> {
                                statusLabel.setText("Ошибка: " + e.getMessage());
                                Alert alert = new Alert(Alert.AlertType.ERROR);
                                alert.setTitle("Ошибка");
                                alert.setHeaderText(null);
                                alert.setContentText("Ошибка создания экземпляра: " + e.getMessage());
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
        statusLabel.setText("Загрузка диаграмм...");
        new Thread(() -> {
            try {
                java.util.List<ProcessModel> models = ApiService.getAllProcessModels();
                Platform.runLater(() -> {
                    if (models == null || models.isEmpty()) {
                        diagramSelector.setItems(FXCollections.emptyObservableList());
                        statusLabel.setText("Нет диаграмм в базе данных");
                        return;
                    }
                    diagramSelector.setItems(FXCollections.observableArrayList(
                            models.stream().map(ProcessModel::name).toList()));
                    statusLabel.setText("Доступно: " + models.size() + " диаграмм");
                    diagramSelector.setOnAction(e -> onDiagramSelected());
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Ошибка загрузки диаграмм: " + e.getMessage());
                });
            }
        }).start();
    }

    private void onDiagramSelected() {
        String selectedName = diagramSelector.getValue();
        if (selectedName == null) return;

        statusLabel.setText("Загрузка " + selectedName + "...");
        new Thread(() -> {
            try {
                ProcessModel model = ApiService.findProcessModelByName(selectedName);
                if (model == null) {
                    Platform.runLater(() -> statusLabel.setText("Диаграмма не найдена"));
                    return;
                }
                currentModel = model;
                Platform.runLater(() -> showDiagram(model));
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Ошибка загрузки: " + e.getMessage()));
            }
        }).start();
    }

    private void showDiagram(ProcessModel model) {
        Path bpmnFile = BPMN_DIR.resolve(model.name() + ".bpmn");
        String xml;
        if (Files.exists(bpmnFile)) {
            try {
                xml = Files.readString(bpmnFile, StandardCharsets.UTF_8);
                statusLabel.setText("Загружено из локального файла");
            } catch (IOException e) {
                xml = model.bpmnXml();
                try {
                    Files.writeString(bpmnFile, xml, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
                    statusLabel.setText("Восстановлено из БД в локальный файл");
                } catch (IOException ex) {
                    statusLabel.setText("Используется BPMN из БД (не удалось сохранить файл)");
                }
            }
        } else {
            xml = model.bpmnXml();
            try {
            Files.createDirectories(BPMN_DIR);
                Files.writeString(bpmnFile, xml, StandardCharsets.UTF_8);
                statusLabel.setText("Создано из БД и сохранено локально");
            } catch (IOException e) {
                statusLabel.setText("Using BPMN from DB (could not save file)");
            }
        }

        if (viewerReady) {
            viewerEngine.executeScript("renderDiagram(`" + escapeForJs(xml) + "`)");
        }

        showProcessInfo(model);
        showTaskDefinitions(model);
    }

    private void showProcessInfo(ProcessModel model) {
        processInfoRows.getChildren().clear();
        addInfoItem("Название", model.name());
        addInfoItem("Версия", String.valueOf(model.version()));
        addInfoItem("Автор", model.authorId() != null ? model.authorId().toString() : "Н/Д");
        addInfoItem("Кол-во задач", model.taskDefinitions() != null ? String.valueOf(model.taskDefinitions().size()) : "0");
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
        private final Long id;
        private final String name;
        private final Integer duration;
        private final BigDecimal cost;
        private final BigDecimal kpiWeight;

        public TaskDefinitionRow(Long id, String name, Integer duration, BigDecimal cost, BigDecimal kpiWeight) {
            this.id = id;
            this.name = name;
            this.duration = duration;
            this.cost = cost;
            this.kpiWeight = kpiWeight;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public Integer getDuration() { return duration; }
        public BigDecimal getCost() { return cost; }
        public BigDecimal getKpiWeight() { return kpiWeight != null ? kpiWeight : BigDecimal.ZERO; }
    }

    private void openEditDialog(TaskDefinitionRow row) {
        Dialog<TaskDefinitionRow> dialog = new Dialog<>();
        dialog.setTitle("Редактирование задачи");
        dialog.setHeaderText("Редактирование задачи #" + row.getId());

        ButtonType saveBtn = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        TextField nameField = new TextField(row.getName());
        TextField durationField = new TextField(String.valueOf(row.getDuration()));
        TextField costField = new TextField(row.getCost().toPlainString());
        TextField kpiField = new TextField(row.getKpiWeight().toPlainString());

        nameField.setPromptText("Название");
        durationField.setPromptText("Длительность (ч)");
        costField.setPromptText("Стоимость ($)");
        kpiField.setPromptText("Вес KPI");

        durationField.setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().matches("\\d*") ? c : null));
        costField.setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().matches("\\d*(\\.\\d{0,2})?") ? c : null));
        kpiField.setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().matches("\\d*\\.?\\d*") ? c : null));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(new Label("Название:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Длительность (ч):"), 0, 1);
        grid.add(durationField, 1, 1);
        grid.add(new Label("Стоимость ($):"), 0, 2);
        grid.add(costField, 1, 2);
        grid.add(new Label("Вес KPI:"), 0, 3);
        grid.add(kpiField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                try {
                    int durationHours = durationField.getText().isBlank() ? 0 : Integer.parseInt(durationField.getText());
                    BigDecimal cost = costField.getText().isBlank() ? BigDecimal.ZERO : new BigDecimal(costField.getText());
                    BigDecimal kpi = kpiField.getText().isBlank() ? BigDecimal.ZERO : new BigDecimal(kpiField.getText());
                    return new TaskDefinitionRow(
                            row.getId(),
                            nameField.getText(),
                            durationHours,
                            cost,
                            kpi
                    );
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updated -> {
            statusLabel.setText("Обновление задачи...");
            new Thread(() -> {
                try {
                    TaskDefinition result = ApiService.updateTaskDefinition(
                            updated.getId(),
                            updated.getName(),
                            updated.getDuration() * 60,
                            updated.getCost(),
                            updated.getKpiWeight()
                    );
                    ProcessModel fresh = ApiService.findProcessModelByName(currentModel.name());
                    Platform.runLater(() -> {
                        statusLabel.setText("Задача обновлена");
                        if (fresh != null) {
                            currentModel = fresh;
                            Path bpmnFile = BPMN_DIR.resolve(currentModel.name() + ".bpmn");
                            try {
                                Files.deleteIfExists(bpmnFile);
                            } catch (IOException ignored) {}
                            showDiagram(currentModel);
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> statusLabel.setText("Ошибка: " + e.getMessage()));
                }
            }).start();
        });
    }

    private void showTaskDefinitions(ProcessModel model) {
        java.util.List<TaskDefinitionRow> rows = new java.util.ArrayList<>();
        if (model.taskDefinitions() != null) {
            for (TaskDefinition td : model.taskDefinitions()) {
                BigDecimal kpiW = td.getKpiWeight();
                rows.add(new TaskDefinitionRow(td.id(), td.name(), td.defaultDuration() / 60, td.expectedCost(), kpiW));
            }
        }
        rows.sort(java.util.Comparator.comparing(TaskDefinitionRow::getId));
        taskTable.setItems(FXCollections.observableArrayList(rows));
    }
}

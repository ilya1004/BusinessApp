package oll.businessdesktop;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import oll.businessdesktop.model.Department;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DepartmentsManagementController {

    @FXML private TreeView<Department> departmentsTree;

    private final List<Department> allDepartments = new ArrayList<>();
    private final Map<Long, Department> departmentMap = new HashMap<>();

    @FXML
    public void initialize() {
        departmentsTree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(Department dept, boolean empty) {
                super.updateItem(dept, empty);
                setText(empty || dept == null ? null : dept.name());
            }
        });
        loadDepartments();
    }

    @FXML
    private void onCreateDepartment() {
        Dialog<Department> dialog = new Dialog<>();
        dialog.setTitle("Создать отдел");
        dialog.setHeaderText("Введите данные нового отдела");

        ButtonType createButtonType = new ButtonType("Создать", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Название");

        ComboBox<Department> parentBox = new ComboBox<>();
        parentBox.getItems().addAll(allDepartments);
        parentBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Department d) {
                return d == null ? "Нет родителя" : d.name();
            }
            @Override
            public Department fromString(String s) { return null; }
        });

        grid.add(new Label("Название:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Родитель:"), 0, 1);
        grid.add(parentBox, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                if (nameField.getText().isBlank()) {
                    showAlert("Ошибка", "Введите название");
                    return null;
                }
                try {
                    Department selectedParent = parentBox.getValue();
                    return ApiService.createDepartment(
                            nameField.getText().trim(),
                            selectedParent != null ? selectedParent.id() : null
                    );
                } catch (Exception e) {
                    showAlert("Ошибка создания", e.getMessage());
                    return null;
                }
            }
            return null;
        });

        if (dialog.showAndWait().orElse(null) != null) {
            loadDepartments();
        }
    }

    @FXML
    private void onEditDepartment() {
        TreeItem<Department> selected = departmentsTree.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue() == null || selected.getValue().id() == null) {
            showAlert("Предупреждение", "Выберите отдел для редактирования");
            return;
        }
        Department dept = selected.getValue();

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Редактировать отдел");
        dialog.setHeaderText("Редактирование отдела");

        ButtonType saveButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField nameField = new TextField(dept.name());
        nameField.setPromptText("Название");

        ComboBox<Department> parentBox = new ComboBox<>();
        parentBox.getItems().addAll(allDepartments);
        parentBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Department d) {
                return d == null ? "Нет родителя" : d.name();
            }
            @Override
            public Department fromString(String s) { return null; }
        });
        parentBox.setValue(dept.parentId() != null ? departmentMap.get(dept.parentId()) : null);

        grid.add(new Label("Название:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Родитель:"), 0, 1);
        grid.add(parentBox, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                if (nameField.getText().isBlank()) {
                    showAlert("Ошибка", "Введите название");
                    return false;
                }
                try {
                    Department selectedParent = parentBox.getValue();
                    ApiService.updateDepartment(
                            dept.id(),
                            nameField.getText().trim(),
                            selectedParent != null ? selectedParent.id() : null
                    );
                    return true;
                } catch (Exception e) {
                    showAlert("Ошибка обновления", e.getMessage());
                    return false;
                }
            }
            return false;
        });

        if (dialog.showAndWait().orElse(false)) {
            loadDepartments();
        }
    }

    @FXML
    private void onDeleteDepartment() {
        TreeItem<Department> selected = departmentsTree.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue() == null || selected.getValue().id() == null) {
            showAlert("Предупреждение", "Выберите отдел для удаления");
            return;
        }
        Department dept = selected.getValue();

        try {
            List<Department> children = ApiService.getDepartmentChildren(dept.id());
            String childrenNames = children.stream().map(Department::name).collect(Collectors.joining(", "));

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Удалить отдел");
            confirm.setHeaderText("Удалить отдел " + dept.name() + "?");

            if (!children.isEmpty()) {
                confirm.setContentText("Отдел имеет дочерние отделы: " + childrenNames +
                        ".\n\nПри удалении дочерние отделы будут переназначены родительскому отделу удаляемого (или станут корневыми, если родителя нет).");
            } else {
                confirm.setContentText("Это действие нельзя отменить.");
            }

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        ApiService.deleteDepartment(dept.id());
                        loadDepartments();
                    } catch (Exception e) {
                        showAlert("Ошибка удаления", e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось проверить дочерние отделы: " + e.getMessage());
        }
    }

    private void loadDepartments() {
        Platform.runLater(() -> {
            try {
                allDepartments.clear();
                departmentMap.clear();
                List<Department> depts = ApiService.getAllDepartments();
                allDepartments.addAll(depts);
                for (Department d : depts) {
                    departmentMap.put(d.id(), d);
                }

                TreeItem<Department> root = new TreeItem<>();
                root.setValue(new Department(null, "Организация", null));
                Map<Long, TreeItem<Department>> itemMap = new HashMap<>();

                for (Department d : depts) {
                    TreeItem<Department> item = new TreeItem<>(d);
                    itemMap.put(d.id(), item);
                }

                for (Department d : depts) {
                    TreeItem<Department> item = itemMap.get(d.id());
                    if (d.parentId() != null && itemMap.containsKey(d.parentId())) {
                        itemMap.get(d.parentId()).getChildren().add(item);
                    } else {
                        root.getChildren().add(item);
                    }
                }

                departmentsTree.setRoot(root);
                root.setExpanded(true);
                expandAll(root);
            } catch (Exception e) {
                showAlert("Ошибка загрузки", e.getMessage());
            }
        });
    }

    private void expandAll(TreeItem<Department> item) {
        item.setExpanded(true);
        for (TreeItem<Department> child : item.getChildren()) {
            expandAll(child);
        }
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}

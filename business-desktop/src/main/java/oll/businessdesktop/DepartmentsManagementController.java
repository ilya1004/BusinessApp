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
        dialog.setTitle("Create Department");
        dialog.setHeaderText("Enter new department details");

        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Name");

        ComboBox<Department> parentBox = new ComboBox<>();
        parentBox.getItems().addAll(allDepartments);
        parentBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Department d) {
                return d == null ? "No parent" : d.name();
            }
            @Override
            public Department fromString(String s) { return null; }
        });

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Parent:"), 0, 1);
        grid.add(parentBox, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                if (nameField.getText().isBlank()) {
                    showAlert("Error", "Enter a name");
                    return null;
                }
                try {
                    Department selectedParent = parentBox.getValue();
                    return ApiService.createDepartment(
                            nameField.getText().trim(),
                            selectedParent != null ? selectedParent.id() : null
                    );
                } catch (Exception e) {
                    showAlert("Creation Error", e.getMessage());
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
            showAlert("Warning", "Select a department to edit");
            return;
        }
        Department dept = selected.getValue();

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Edit Department");
        dialog.setHeaderText("Edit department details");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField nameField = new TextField(dept.name());
        nameField.setPromptText("Name");

        ComboBox<Department> parentBox = new ComboBox<>();
        parentBox.getItems().addAll(allDepartments);
        parentBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Department d) {
                return d == null ? "No parent" : d.name();
            }
            @Override
            public Department fromString(String s) { return null; }
        });
        parentBox.setValue(dept.parentId() != null ? departmentMap.get(dept.parentId()) : null);

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Parent:"), 0, 1);
        grid.add(parentBox, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                if (nameField.getText().isBlank()) {
                    showAlert("Error", "Enter a name");
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
                    showAlert("Update Error", e.getMessage());
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
            showAlert("Warning", "Select a department to delete");
            return;
        }
        Department dept = selected.getValue();

        try {
            List<Department> children = ApiService.getDepartmentChildren(dept.id());
            String childrenNames = children.stream().map(Department::name).collect(Collectors.joining(", "));

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete Department");
            confirm.setHeaderText("Delete department " + dept.name() + "?");

            if (!children.isEmpty()) {
                confirm.setContentText("The department has children: " + childrenNames +
                        ".\n\nWhen deleted, child departments will be reassigned to the parent department of the deleted one (or become root-level if there is no parent).");
            } else {
                confirm.setContentText("This action cannot be undone.");
            }

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        ApiService.deleteDepartment(dept.id());
                        loadDepartments();
                    } catch (Exception e) {
                        showAlert("Delete Error", e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            showAlert("Error", "Could not check child departments: " + e.getMessage());
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
                root.setValue(new Department(null, "Organization", null));
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
                showAlert("Load Error", e.getMessage());
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

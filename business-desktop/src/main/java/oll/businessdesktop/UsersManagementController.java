package oll.businessdesktop;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import oll.businessdesktop.model.Department;
import oll.businessdesktop.model.User;

import java.util.ArrayList;
import java.util.List;

public class UsersManagementController {

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Long> idColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> firstNameColumn;
    @FXML private TableColumn<User, String> lastNameColumn;
    @FXML private TableColumn<User, String> departmentColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, Void> actionsColumn;

    private List<Department> allDepartments = new ArrayList<>();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().id()));
        usernameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().username()));
        firstNameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().firstName()));
        lastNameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().lastName()));
        departmentColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().departmentName()));
        roleColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().role()));

        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final HBox box = new HBox(6, editButton, deleteButton);

            {
                editButton.getStyleClass().add("table-edit-button");
                editButton.setOnAction(e -> {
                    int idx = getIndex();
                    if (idx >= 0 && idx < getTableView().getItems().size()) {
                        editUser(getTableView().getItems().get(idx));
                    }
                });
                deleteButton.getStyleClass().add("table-delete-button");
                deleteButton.setOnAction(e -> {
                    int idx = getIndex();
                    if (idx >= 0 && idx < getTableView().getItems().size()) {
                        confirmDelete(getTableView().getItems().get(idx));
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        });

        idColumn.setSortType(TableColumn.SortType.ASCENDING);
        usersTable.getSortOrder().add(idColumn);

        loadDepartments();
        loadUsers();
    }

    private void loadDepartments() {
        Platform.runLater(() -> {
            try {
                allDepartments = ApiService.getAllDepartments();
            } catch (Exception e) {
                allDepartments = new ArrayList<>();
            }
        });
    }

    private void confirmDelete(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete User");
        confirm.setHeaderText("Delete user " + user.username() + "?");
        confirm.setContentText("This action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    ApiService.deleteUser(user.id());
                    loadUsers();
                } catch (Exception e) {
                    showAlert("Delete Error", e.getMessage());
                }
            }
        });
    }

    @FXML
    private void onCreateUser() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Create User");
        dialog.setHeaderText("Enter new user details");

        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        TextField firstNameField = new TextField();
        firstNameField.setPromptText("First Name");
        TextField lastNameField = new TextField();
        lastNameField.setPromptText("Last Name");
        ChoiceBox<String> roleChoiceBox = new ChoiceBox<>();
        try {
            List<String> roles = ApiService.getRoles();
            roleChoiceBox.getItems().addAll(roles);
            roleChoiceBox.setValue(roles.isEmpty() ? null : roles.get(0));
        } catch (Exception e) {
            showAlert("Error", "Could not load roles: " + e.getMessage());
        }

        ComboBox<Department> departmentBox = new ComboBox<>();
        departmentBox.getItems().add(null);
        departmentBox.getItems().addAll(allDepartments);
        departmentBox.setValue(null);
        departmentBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Department d, boolean empty) {
                super.updateItem(d, empty);
                setText(d == null ? "No Department" : d.name());
            }
        });
        departmentBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Department d, boolean empty) {
                super.updateItem(d, empty);
                setText(d == null ? "No Department" : d.name());
            }
        });

        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(new Label("First Name:"), 0, 2);
        grid.add(firstNameField, 1, 2);
        grid.add(new Label("Last Name:"), 0, 3);
        grid.add(lastNameField, 1, 3);
        grid.add(new Label("Department:"), 0, 4);
        grid.add(departmentBox, 1, 4);
        grid.add(new Label("Role:"), 0, 5);
        grid.add(roleChoiceBox, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                if (usernameField.getText().isBlank() || passwordField.getText().isBlank() ||
                    firstNameField.getText().isBlank() || lastNameField.getText().isBlank()) {
                    showAlert("Error", "Fill in all fields");
                    return null;
                }
                if (roleChoiceBox.getValue() == null) {
                    showAlert("Error", "Select a role");
                    return null;
                }
                try {
                    Long deptId = departmentBox.getValue() != null ? departmentBox.getValue().id() : null;
                    ApiService.createUser(
                            usernameField.getText().trim(),
                            passwordField.getText(),
                            roleChoiceBox.getValue(),
                            firstNameField.getText().trim(),
                            lastNameField.getText().trim(),
                            deptId
                    );
                    loadUsers();
                    return true;
                } catch (Exception e) {
                    showAlert("Creation Error", e.getMessage());
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void editUser(User user) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Edit User");
        dialog.setHeaderText("Edit user: " + user.username());

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField firstNameField = new TextField(user.firstName());
        TextField lastNameField = new TextField(user.lastName());
        ChoiceBox<String> roleChoiceBox = new ChoiceBox<>();
        try {
            List<String> roles = ApiService.getRoles();
            roleChoiceBox.getItems().addAll(roles);
            roleChoiceBox.setValue(user.role());
        } catch (Exception e) {
            showAlert("Error", "Could not load roles: " + e.getMessage());
        }

        ComboBox<Department> departmentBox = new ComboBox<>();
        departmentBox.getItems().add(null);
        departmentBox.getItems().addAll(allDepartments);
        departmentBox.setValue(user.department());
        departmentBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Department d, boolean empty) {
                super.updateItem(d, empty);
                setText(d == null ? "No Department" : d.name());
            }
        });
        departmentBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Department d, boolean empty) {
                super.updateItem(d, empty);
                setText(d == null ? "No Department" : d.name());
            }
        });

        grid.add(new Label("First Name:"), 0, 0);
        grid.add(firstNameField, 1, 0);
        grid.add(new Label("Last Name:"), 0, 1);
        grid.add(lastNameField, 1, 1);
        grid.add(new Label("Department:"), 0, 2);
        grid.add(departmentBox, 1, 2);
        grid.add(new Label("Role:"), 0, 3);
        grid.add(roleChoiceBox, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                if (firstNameField.getText().isBlank() || lastNameField.getText().isBlank()) {
                    showAlert("Error", "Fill in all fields");
                    return null;
                }
                if (roleChoiceBox.getValue() == null) {
                    showAlert("Error", "Select a role");
                    return null;
                }
                try {
                    Long deptId = departmentBox.getValue() != null ? departmentBox.getValue().id() : null;
                    ApiService.updateUser(
                            user.id(),
                            firstNameField.getText().trim(),
                            lastNameField.getText().trim(),
                            roleChoiceBox.getValue(),
                            deptId
                    );
                    loadUsers();
                    return true;
                } catch (Exception e) {
                    showAlert("Edit Error", e.getMessage());
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void loadUsers() {
        Platform.runLater(() -> {
            try {
                List<User> users = ApiService.getAllUsers();
                usersTable.getItems().setAll(users);
            } catch (Exception e) {
                showAlert("Load Error", e.getMessage());
            }
        });
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

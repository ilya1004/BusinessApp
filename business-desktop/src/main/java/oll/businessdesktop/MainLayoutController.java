package oll.businessdesktop;

import atlantafx.base.theme.CupertinoDark;
import atlantafx.base.theme.CupertinoLight;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.util.List;

public class MainLayoutController {

    @FXML private BorderPane rootLayout;
    @FXML private BorderPane contentArea;
    @FXML private javafx.scene.control.Label pageTitle;
    @FXML private Button btnProcessDesigner;
    @FXML private Button btnProcessView;
    @FXML private Button btnProcessInstances;
    @FXML private Button btnSimulation;
    @FXML private Button btnMyTasks;
    @FXML private Button btnUsers;
    @FXML private Button btnDepartments;
    @FXML private Button btnLogs;

    private static MainLayoutController instance;
    private boolean isDarkTheme = false;

    @FXML
    public void initialize() {
        instance = this;
        String role = ApiService.getCurrentUserRole();
        applyRoleVisibility(role);
        navigateToFirstAvailable(role);
    }

    private void applyRoleVisibility(String role) {
        if (role == null) return;
        allButtons().forEach(b -> { b.setVisible(false); b.setManaged(false); });
        switch (role) {
            case "ADMIN" -> show(btnProcessDesigner, btnProcessView, btnProcessInstances, btnSimulation, btnUsers, btnDepartments, btnLogs);
            case "MANAGER" -> show(btnProcessView, btnProcessInstances, btnSimulation);
            case "ANALYST" -> show(btnProcessDesigner, btnProcessView, btnProcessInstances, btnSimulation);
            case "EXECUTOR" -> show(btnMyTasks);
        }
    }

    private void show(Button... buttons) {
        for (Button b : buttons) {
            b.setVisible(true);
            b.setManaged(true);
        }
    }

    private List<Button> allButtons() {
        return List.of(btnProcessDesigner, btnProcessView, btnProcessInstances, btnSimulation, btnMyTasks, btnUsers, btnDepartments, btnLogs);
    }

    private void navigateToFirstAvailable(String role) {
        if (role == null) return;
        switch (role) {
            case "MANAGER" -> onProcessViewTab();
            case "EXECUTOR" -> onMyTasksTab();
            default -> onProcessDesignerTab();
        }
    }

    public static void navigateToInstances(Long instanceId) {
        if (instance != null) {
            instance.onProcessInstancesTab(instanceId);
        }
    }

    @FXML
    private void onProcessDesignerTab() {
        pageTitle.setText("Редактор процессов");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/oll/businessdesktop/process-designer-view.fxml"));
            Pane view = loader.load();
            contentArea.setCenter(view);
            BorderPane.setMargin(view, new javafx.geometry.Insets(0));
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Ошибка загрузки редактора процессов: " + e.getMessage());
        }
    }

    @FXML
    private void onProcessViewTab() {
        pageTitle.setText("Модели процессов");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/oll/businessdesktop/process-view-view.fxml"));
            Pane view = loader.load();
            contentArea.setCenter(view);
            BorderPane.setMargin(view, new javafx.geometry.Insets(0));
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Ошибка загрузки просмотра процессов: " + e.getMessage());
        }
    }

    @FXML
    private void onTab2() {
        pageTitle.setText("Вкладка 2");
        loadPlaceholder("Вкладка 2");
    }

    @FXML
    private void onTab3() {
        pageTitle.setText("Вкладка 3");
        loadPlaceholder("Вкладка 3");
    }

    @FXML
    private void onProfileClick() {
        pageTitle.setText("Профиль пользователя");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/oll/businessdesktop/profile-view.fxml"));
            Pane view = loader.load();
            contentArea.setCenter(view);
            BorderPane.setMargin(view, new javafx.geometry.Insets(0));
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Ошибка загрузки профиля: " + e.getMessage());
        }
    }

    @FXML
    private void onUsersTab() {
        pageTitle.setText("Управление пользователями");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/oll/businessdesktop/users-management-view.fxml"));
            Pane view = loader.load();
            contentArea.setCenter(view);
            BorderPane.setMargin(view, new javafx.geometry.Insets(0));
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Ошибка загрузки страницы: " + e.getMessage());
        }
    }

    @FXML
    private void onDepartmentsTab() {
        pageTitle.setText("Управление организацией");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/oll/businessdesktop/departments-management-view.fxml"));
            Pane view = loader.load();
            contentArea.setCenter(view);
            BorderPane.setMargin(view, new javafx.geometry.Insets(0));
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Ошибка загрузки страницы: " + e.getMessage());
        }
    }

    @FXML
    private void onProcessInstancesTab() {
        onProcessInstancesTab(null);
    }

    private void onProcessInstancesTab(Long instanceId) {
        pageTitle.setText("Процессы");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/oll/businessdesktop/process-instances-view.fxml"));
            Pane view = loader.load();
            ProcessInstancesController controller = loader.getController();
            if (controller != null && instanceId != null) {
                controller.selectInstanceById(instanceId);
            }
            contentArea.setCenter(view);
            BorderPane.setMargin(view, new javafx.geometry.Insets(0));
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Ошибка загрузки экземпляров процессов: " + e.getMessage());
        }
    }

    @FXML
    private void onSimulationTab() {
        pageTitle.setText("Симуляция сценариев");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/oll/businessdesktop/simulation-view.fxml"));
            Pane view = loader.load();
            contentArea.setCenter(view);
            BorderPane.setMargin(view, new javafx.geometry.Insets(0));
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Ошибка загрузки симуляции: " + e.getMessage());
        }
    }

    @FXML
    private void onMyTasksTab() {
        pageTitle.setText("Мои задачи");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/oll/businessdesktop/my-tasks-view.fxml"));
            Pane view = loader.load();
            contentArea.setCenter(view);
            BorderPane.setMargin(view, new javafx.geometry.Insets(0));
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Ошибка загрузки задач: " + e.getMessage());
        }
    }

    @FXML
    private void onLogsTab() {
        pageTitle.setText("Логи сервера");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/oll/businessdesktop/logs-view.fxml"));
            Pane view = loader.load();
            contentArea.setCenter(view);
            BorderPane.setMargin(view, new javafx.geometry.Insets(0));
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Ошибка загрузки логов: " + e.getMessage());
        }
    }

    @FXML
    private void onToggleTheme() {
        isDarkTheme = !isDarkTheme;
        Platform.runLater(() -> {
            if (isDarkTheme) {
                Application.setUserAgentStylesheet(new CupertinoDark().getUserAgentStylesheet());
            } else {
                Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());
            }
        });
    }

    private void loadPlaceholder(String tabName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/oll/businessdesktop/placeholder-view.fxml"));
            Pane view = loader.load();
            PlaceholderController controller = loader.getController();
            if (controller != null) {
                controller.setTabName(tabName);
            }
            contentArea.setCenter(view);
            BorderPane.setMargin(view, new javafx.geometry.Insets(0));
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Ошибка загрузки страницы: " + e.getMessage());
        }
    }

    private void showAlert(String message) {
        javafx.application.Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, message).show());
    }
}

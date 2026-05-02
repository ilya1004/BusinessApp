package oll.businessdesktop;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

import java.io.IOException;

public class MainLayoutController {

    @FXML private BorderPane rootLayout;
    @FXML private BorderPane contentArea;
    @FXML private javafx.scene.control.Label pageTitle;

    @FXML
    public void initialize() {
        loadProcessDesigner();
    }

    @FXML
    private void onBpmnTab() {
        loadProcessDesigner();
    }

    @FXML
    private void onProcessDesignerTab() {
        pageTitle.setText("Process Designer");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/oll/businessdesktop/process-designer-view.fxml"));
            Pane view = loader.load();
            contentArea.setCenter(view);
            BorderPane.setMargin(view, new javafx.geometry.Insets(0));
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Process Designer load error: " + e.getMessage());
        }
    }

    @FXML
    private void onTab2() {
        pageTitle.setText("Tab 2");
        loadPlaceholder("Tab 2");
    }

    @FXML
    private void onTab3() {
        pageTitle.setText("Tab 3");
        loadPlaceholder("Tab 3");
    }

    @FXML
    private void onProfileClick() {
        pageTitle.setText("User Profile");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/oll/businessdesktop/profile-view.fxml"));
            Pane view = loader.load();
            contentArea.setCenter(view);
            BorderPane.setMargin(view, new javafx.geometry.Insets(0));
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Profile load error: " + e.getMessage());
        }
    }

    private void loadProcessDesigner() {
        pageTitle.setText("BPMN Editor");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/oll/businessdesktop/process-designer-view.fxml"));
            Pane view = loader.load();
            contentArea.setCenter(view);
            BorderPane.setMargin(view, new javafx.geometry.Insets(0));
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("BPMN editor load error: " + e.getMessage());
        }
    }

    @FXML
    private void onUsersTab() {
        pageTitle.setText("User Management");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/oll/businessdesktop/users-management-view.fxml"));
            Pane view = loader.load();
            contentArea.setCenter(view);
            BorderPane.setMargin(view, new javafx.geometry.Insets(0));
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Page load error: " + e.getMessage());
        }
    }

    @FXML
    private void onDepartmentsTab() {
        pageTitle.setText("Organization Management");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/oll/businessdesktop/departments-management-view.fxml"));
            Pane view = loader.load();
            contentArea.setCenter(view);
            BorderPane.setMargin(view, new javafx.geometry.Insets(0));
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Page load error: " + e.getMessage());
        }
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
            showAlert("Page load error: " + e.getMessage());
        }
    }

    private void showAlert(String message) {
        javafx.application.Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, message).show());
    }
}

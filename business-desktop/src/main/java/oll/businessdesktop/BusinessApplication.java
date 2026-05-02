package oll.businessdesktop;

import atlantafx.base.theme.CupertinoDark;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class BusinessApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Apply AtlantaFX theme
            Platform.runLater(() -> Application.setUserAgentStylesheet(new CupertinoDark().getUserAgentStylesheet()));

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/oll/businessdesktop/login-view.fxml"));
            Scene scene = new Scene(loader.load(), 400, 500);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Business Desktop - Login");
            primaryStage.setResizable(false);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

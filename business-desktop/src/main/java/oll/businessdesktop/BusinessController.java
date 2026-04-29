package oll.businessdesktop;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class BusinessController {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }
}

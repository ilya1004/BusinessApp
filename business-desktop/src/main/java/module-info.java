module oll.businessdesktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires jdk.jsobject;


    opens oll.businessdesktop to javafx.fxml;
    exports oll.businessdesktop;
}
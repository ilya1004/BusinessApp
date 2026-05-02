module oll.businessdesktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires jdk.jsobject;
    requires atlantafx.base;
    requires com.fasterxml.jackson.databind;
    requires java.net.http;

    opens oll.businessdesktop to javafx.fxml, atlantafx.base;
    opens oll.businessdesktop.model to javafx.fxml, com.fasterxml.jackson.databind;
    exports oll.businessdesktop;
    exports oll.businessdesktop.model;
}
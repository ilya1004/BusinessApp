module oll.businessdesktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires jdk.jsobject;
    requires atlantafx.base;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires java.net.http;
    requires java.desktop;

    opens oll.businessdesktop to javafx.fxml, javafx.web, atlantafx.base;
    opens oll.businessdesktop.model to javafx.fxml, com.fasterxml.jackson.databind;
    exports oll.businessdesktop;
    exports oll.businessdesktop.model;
}
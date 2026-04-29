package oll.businessdesktop;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class BusinessApplication extends Application {
    private WebEngine engine;

    @Override
    public void start(Stage primaryStage) {
        WebView webView = new WebView();
        engine = webView.getEngine();

        // Мост Java ↔ JavaScript
        JavaBridge bridge = new JavaBridge();

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaBridge", bridge);
            }
        });

        engine.load(getClass().getResource("/bpmn-editor.html").toExternalForm());

        // ==================== Панель инструментов ====================
        ToolBar toolBar = new ToolBar();

        Button btnNew = new Button("Новая диаграмма");
        Button btnOpen = new Button("Открыть .bpmn");
        Button btnSave = new Button("Сохранить XML");
        Button btnExportSVG = new Button("Экспорт SVG");

        btnNew.setOnAction(e -> engine.executeScript("javaBridge.importXML(`" + getEmptyBpmn() + "`);"));
        btnOpen.setOnAction(e -> openFile(primaryStage));
        btnSave.setOnAction(e -> engine.executeScript("javaBridge.saveXML();"));
        btnExportSVG.setOnAction(e -> engine.executeScript("javaBridge.exportSVG();"));

        toolBar.getItems().addAll(btnNew, btnOpen, btnSave, new Separator(), btnExportSVG);

        BorderPane root = new BorderPane();
        root.setTop(toolBar);
        root.setCenter(webView);

        Scene scene = new Scene(root, 1280, 800);
        primaryStage.setScene(scene);
        primaryStage.setTitle("BPMN Modeler (bpmn-js + JavaFX)");
        primaryStage.show();
    }

    private String getEmptyBpmn() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" " +
                "xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" " +
                "id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n" +
                "  <bpmn:process id=\"Process_1\" isExecutable=\"true\"/>\n" +
                "  <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\"/>\n" +
                "</bpmn:definitions>";
    }

    private void openFile(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("BPMN файлы", "*.bpmn", "*.xml"));
        File file = chooser.showOpenDialog(stage);

        if (file != null) {
            try {
                String xml = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                // Экранируем обратные кавычки
                xml = xml.replace("`", "\\`");
                engine.executeScript("javaBridge.importXML(`" + xml + "`);");
            } catch (Exception ex) {
                ex.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось открыть файл");
                alert.show();
            }
        }
    }

    // ==================== Класс-мост ====================
    public class JavaBridge {

        public void onXMLSaved(String xml) {
            System.out.println("BPMN XML получен (" + xml.length() + " символов)");

            // Сохраняем в файл
            try (FileWriter writer = new FileWriter("my-diagram.bpmn")) {
                writer.write(xml);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Диаграмма сохранена в my-diagram.bpmn");
                alert.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void onSVGExported(String svg) {
            try (FileWriter writer = new FileWriter("diagram.svg")) {
                writer.write(svg);
                System.out.println("SVG успешно экспортирован");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

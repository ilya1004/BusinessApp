package oll.businessdesktop;

import javafx.application.Application;
import javafx.application.Platform;
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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class BusinessApplication extends Application {
    private WebEngine engine;
    private JavaBridge bridge;
    private static final Path LOG_DIR = Paths.get("js-logs");
    private static final Path LOG_FILE = LOG_DIR.resolve("console.log");

    @Override
    public void start(Stage primaryStage) {
        WebView webView = new WebView();
        engine = webView.getEngine();

        // Инициализация моста с файловым логгером
        try {
            bridge = new JavaBridge(LOG_FILE);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Не удалось создать лог-файл:\n" + e.getMessage());
            return;
        }

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                System.out.println("✅ Страница загружена. Регистрация javaBridge...");
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaBridge", bridge);

                // Активируем JS-консольный мост
                try {
                    engine.executeScript("if (typeof window.__enableConsoleBridge === 'function') window.__enableConsoleBridge();");
                } catch (Exception e) {
                    System.err.println("⚠️ Не удалось активировать консольный мост: " + e.getMessage());
                }
            } else if (newState == javafx.concurrent.Worker.State.FAILED) {
                System.err.println("❌ Ошибка загрузки страницы: " + engine.getLoadWorker().getException());
            }
        });

        engine.load(Objects.requireNonNull(getClass().getResource("/bpmn-editor.html")).toExternalForm());

        // ==================== Панель инструментов ====================
        ToolBar toolBar = new ToolBar();
        Button btnNew = new Button("Новая диаграмма");
        Button btnOpen = new Button("Открыть .bpmn");
        Button btnSave = new Button("Сохранить XML");
        Button btnExportSVG = new Button("Экспорт SVG");

        // В start(), обработчик кнопки "Новая диаграмма":
        btnNew.setOnAction(e -> {
            // Способ 1: Перезагрузка страницы (полный сброс)
            engine.reload();

            // ИЛИ Способ 2: Если хотите сохранить состояние javaBridge после перезагрузки:
            // engine.getLoadWorker().stateProperty().addListener(new ReloadListener());
            // engine.reload();
        });

        btnOpen.setOnAction(e -> openFile(primaryStage));
        btnSave.setOnAction(e -> safeExecScript("saveXML();"));
        btnExportSVG.setOnAction(e -> safeExecScript("exportSVG();"));

        toolBar.getItems().addAll(btnNew, btnOpen, btnSave, new Separator(), btnExportSVG);

        BorderPane root = new BorderPane();
        root.setTop(toolBar);
        root.setCenter(webView);

        Scene scene = new Scene(root, 1280, 800);
        primaryStage.setScene(scene);
        primaryStage.setTitle("BPMN Modeler (bpmn-js + JavaFX)");
        primaryStage.show();
    }

    private void safeExecScript(String script) {
        try {
            engine.executeScript(script);
        } catch (Exception e) {
            System.err.println("❌ JS Error: " + e.getMessage());
        }
    }

    private String escapeForJs(String str) {
        return str.replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("${", "\\${")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
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
                safeExecScript("importXML(`" + escapeForJs(xml) + "`);");
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Не удалось открыть файл: " + ex.getMessage());
            }
        }
    }

    private static void showAlert(Alert.AlertType type, String message) {
        Platform.runLater(() -> new Alert(type, message).show());
    }

    // ==================== JavaBridge + File Logger ====================
    public static class JavaBridge {
        private final BufferedWriter logWriter;
        private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

        public JavaBridge(Path logFile) throws IOException {
            Files.createDirectories(logFile.getParent());
            // CREATE + APPEND + WRITE
            logWriter = Files.newBufferedWriter(logFile, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);

            writeLog("SYSTEM", "Logger initialized → " + logFile.toAbsolutePath());

            // Гарантируем закрытие файла при завершении JVM
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try { logWriter.flush(); logWriter.close(); } catch (IOException ignored) {}
            }));
        }

        /** Вызывается из JavaScript через console bridge */
        public void log(String level, String message) {
            writeLog(level, message);
        }

        private synchronized void writeLog(String level, String message) {
            String line = String.format("[%s] [%-6s] %s%n", LocalDateTime.now().format(dtf), level, message);
            try {
                logWriter.write(line);
                logWriter.flush(); // Немедленная запись (удалите в production для производительности)
            } catch (IOException e) {
                System.err.println("⚠️ Ошибка записи в лог: " + e.getMessage());
            }
        }

        public void onXMLSaved(String xml) {
            writeLog("JAVA", "XML saved (" + xml.length() + " chars)");
            Platform.runLater(() -> {
                try (FileWriter fw = new FileWriter("my-diagram.bpmn", StandardCharsets.UTF_8)) {
                    fw.write(xml);
                    showAlert(Alert.AlertType.INFORMATION, "Диаграмма сохранена в my-diagram.bpmn");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
        }

        public void onSVGExported(String svg) {
            writeLog("JAVA", "SVG exported (" + svg.length() + " chars)");
            try (FileWriter fw = new FileWriter("diagram.svg", StandardCharsets.UTF_8)) {
                fw.write(svg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
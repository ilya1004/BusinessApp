package oll.businessdesktop;

import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
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

public class ProcessDesignerController {

    @FXML private WebView webView;
    @FXML private javafx.scene.control.ToolBar toolBar;

    private WebEngine engine;
    private JavaBridge bridge;
    private static final Path LOG_DIR = Paths.get("js-logs");
    private static final Path LOG_FILE = LOG_DIR.resolve("console.log");

    @FXML
    public void initialize() {
        engine = webView.getEngine();

        try {
            bridge = new JavaBridge(LOG_FILE);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Could not create log file:\n" + e.getMessage());
            return;
        }

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                System.out.println("Page loaded. Registering javaBridge...");
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaBridge", bridge);

                try {
                    engine.executeScript("if (typeof window.__enableConsoleBridge === 'function') window.__enableConsoleBridge();");
                } catch (Exception e) {
                    System.err.println("Could not activate console bridge: " + e.getMessage());
                }
            } else if (newState == Worker.State.FAILED) {
                System.err.println("Page load error: " + engine.getLoadWorker().getException());
            }
        });

        engine.load(Objects.requireNonNull(getClass().getResource("/bpmn-editor.html")).toExternalForm());
    }

    @FXML
    private void onNewDiagram() {
        engine.reload();
    }

    @FXML
    private void onOpenFile() {
        Stage stage = (Stage) webView.getScene().getWindow();
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("BPMN Files", "*.bpmn", "*.xml"));
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            try {
                String xml = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                safeExecScript("importXML(`" + escapeForJs(xml) + "`);");
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Could not open file: " + ex.getMessage());
            }
        }
    }

    @FXML
    private void onSaveXML() {
        safeExecScript("saveXML();");
    }

    @FXML
    private void onExportSVG() {
        safeExecScript("exportSVG();");
    }

    private void safeExecScript(String script) {
        try {
            engine.executeScript(script);
        } catch (Exception e) {
            System.err.println("JS Error: " + e.getMessage());
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

    private static void showAlert(Alert.AlertType type, String message) {
        javafx.application.Platform.runLater(() -> new Alert(type, message).show());
    }

    public static class JavaBridge {
        private final BufferedWriter logWriter;
        private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

        public JavaBridge(Path logFile) throws IOException {
            Files.createDirectories(logFile.getParent());
            logWriter = Files.newBufferedWriter(logFile, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
            writeLog("SYSTEM", "Logger initialized → " + logFile.toAbsolutePath());
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try { logWriter.flush(); logWriter.close(); } catch (IOException ignored) {}
            }));
        }

        public void log(String level, String message) {
            writeLog(level, message);
        }

        private synchronized void writeLog(String level, String message) {
            String line = String.format("[%s] [%-6s] %s%n", LocalDateTime.now().format(dtf), level, message);
            try {
                logWriter.write(line);
                logWriter.flush();
            } catch (IOException e) {
                System.err.println("Log write error: " + e.getMessage());
            }
        }

        public void onXMLSaved(String xml) {
            writeLog("JAVA", "XML saved (" + xml.length() + " chars)");
            javafx.application.Platform.runLater(() -> {
                try (FileWriter fw = new FileWriter("my-diagram.bpmn", StandardCharsets.UTF_8)) {
                    fw.write(xml);
                    showAlert(Alert.AlertType.INFORMATION, "Diagram saved to my-diagram.bpmn");
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
}

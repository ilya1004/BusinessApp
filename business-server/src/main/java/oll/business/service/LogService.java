package oll.business.service;

import oll.business.model.AppLog;
import oll.business.repository.AppLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class LogService {

    private static final Logger logger = LoggerFactory.getLogger(LogService.class);
    private static final String LOG_FILE_PATH = "logs/app.log";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AppLogRepository logRepository;

    public LogService(AppLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    public void logInfo(String message) {
        log("INFO", message, null, null, null, null);
    }

    public void logInfo(String message, String source, String action) {
        log("INFO", message, source, action, null, null);
    }

    public void logInfo(String message, String source, String action, Long userId, String details) {
        log("INFO", message, source, action, userId, details);
    }

    public void logWarn(String message) {
        log("WARN", message, null, null, null, null);
    }

    public void logWarn(String message, String source, String action) {
        log("WARN", message, source, action, null, null);
    }

    public void logWarn(String message, String source, String action, Long userId, String details) {
        log("WARN", message, source, action, userId, details);
    }

    public void logError(String message) {
        log("ERROR", message, null, null, null, null);
    }

    public void logError(String message, String source, String action) {
        log("ERROR", message, source, action, null, null);
    }

    public void logError(String message, String source, String action, Long userId, String details) {
        log("ERROR", message, source, action, userId, details);
    }

    public void log(String level, String message, String source, String action) {
        log(level, message, source, action, null, null);
    }

    public void log(String level, String message, String source, String action, Long userId, String details) {
        AppLog appLog = new AppLog(level, message, source, action);
        appLog.setUserId(userId);
        appLog.setDetails(details);
        logRepository.save(appLog);

        writeToFile(level, message, source, action);

        switch (level) {
            case "ERROR" -> logger.error("[{}] {} | source={}, action={}", source, message, source, action);
            case "WARN" -> logger.warn("[{}] {} | source={}, action={}", source, message, source, action);
            default -> logger.info("[{}] {} | source={}, action={}", source, message, source, action);
        }
    }

    private void writeToFile(String level, String message, String source, String action) {
        try {
            ensureLogDirectory();
            try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE_PATH, true))) {
                String timestamp = LocalDateTime.now().format(FORMATTER);
                writer.println(String.format("[%s] [%s] [%s] %s | action=%s",
                        timestamp, level, source != null ? source : "N/A", message, action != null ? action : "N/A"));
            }
        } catch (IOException e) {
            logger.error("Failed to write log to file: {}", e.getMessage());
        }
    }

    private void ensureLogDirectory() {
        java.io.File logDir = new java.io.File("logs");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
    }

    public Page<AppLog> getLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return logRepository.findByOrderByTimestampDesc(pageable);
    }

    public Page<AppLog> getLogsByLevel(String level, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return logRepository.findByLevelOrderByTimestampDesc(level, pageable);
    }

    public Page<AppLog> getLogsBySource(String source, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return logRepository.findBySourceOrderByTimestampDesc(source, pageable);
    }

    public List<AppLog> getRecentLogs(int limit) {
        return logRepository.findTop100ByOrderByTimestampDesc();
    }

    public long getTotalCount() {
        return logRepository.count();
    }
}
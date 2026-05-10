package oll.business.controller;

import oll.business.model.AppLog;
import oll.business.service.LogService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String source) {

        Page<AppLog> logs;
        if (level != null && !level.isEmpty() && !"ALL".equalsIgnoreCase(level)) {
            logs = logService.getLogsByLevel(level.toUpperCase(), page, size);
        } else if (source != null && !source.isEmpty()) {
            logs = logService.getLogsBySource(source, page, size);
        } else {
            logs = logService.getLogs(page, size);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", logs.getContent());
        response.put("totalPages", logs.getTotalPages());
        response.put("totalElements", logs.getTotalElements());
        response.put("currentPage", logs.getNumber());
        response.put("pageSize", logs.getSize());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<AppLog>> getRecentLogs(
            @RequestParam(defaultValue = "100") int limit) {
        List<AppLog> logs = logService.getRecentLogs(Math.min(limit, 500));
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalLogs", logService.getTotalCount());
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/info")
    public ResponseEntity<Void> logInfo(
            @RequestBody Map<String, String> body,
            Authentication auth) {
        String message = body.get("message");
        String source = body.getOrDefault("source", "CLIENT");
        String action = body.get("action");
        logService.logInfo(message, source, action);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/warn")
    public ResponseEntity<Void> logWarn(@RequestBody Map<String, String> body, Authentication auth) {
        String message = body.get("message");
        String source = body.getOrDefault("source", "CLIENT");
        String action = body.get("action");
        logService.logWarn(message, source, action);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/error")
    public ResponseEntity<Void> logError(@RequestBody Map<String, String> body, Authentication auth) {
        String message = body.get("message");
        String source = body.getOrDefault("source", "CLIENT");
        String action = body.get("action");
        logService.logError(message, source, action);
        return ResponseEntity.ok().build();
    }
}
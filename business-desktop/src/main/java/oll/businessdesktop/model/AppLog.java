package oll.businessdesktop.model;

import java.time.LocalDateTime;

public class AppLog {
    private Long id;
    private LocalDateTime timestamp;
    private String level;
    private String message;
    private String source;
    private String action;
    private Long userId;
    private String details;

    public AppLog() {}

    public Long getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getLevel() { return level; }
    public String getMessage() { return message; }
    public String getSource() { return source; }
    public String getAction() { return action; }
    public Long getUserId() { return userId; }
    public String getDetails() { return details; }
}
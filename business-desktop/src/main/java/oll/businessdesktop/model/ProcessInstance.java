package oll.businessdesktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProcessInstance(
    Long id,
    ProcessModel model,
    String status,
    LocalDateTime startedAt,
    LocalDateTime finishedAt,
    String currentState,
    String name
) {}

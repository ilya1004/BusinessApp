package oll.businessdesktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Department(
    Long id,
    String name,
    Long parentId
) {}

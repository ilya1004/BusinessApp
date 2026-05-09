package oll.businessdesktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record User(
    Long id,
    String username,
    String firstName,
    String lastName,
    String role,
    Department department
) {
    public String departmentName() {
        return department != null ? department.name() : "-";
    }
}

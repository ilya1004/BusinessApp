package oll.business.model;

public enum Role {
    ADMIN("ADMIN"),
    BUSINESS_ANALYST("BUSINESS_ANALYST"),
    MANAGER("MANAGER"),
    WORKER("WORKER");

    private final String value;

    Role(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Role fromValue(String value) {
        for (Role role : Role.values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + value);
    }
}
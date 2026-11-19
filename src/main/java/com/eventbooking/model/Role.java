
package com.eventbooking.model;

public enum Role {
    CLIENT("/client"),
    ADMIN("/admin"),
    AGENT("/agent");

    private final String homePath;

    Role(String homePath) {
        this.homePath = homePath;
    }

    public String getHomePath() {
        return homePath;
    }
}

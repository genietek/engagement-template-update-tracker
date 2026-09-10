package com.caseware.templateupdate.model;

import java.util.Objects;

public record EngagementId(String value) {
    public EngagementId {
        Objects.requireNonNull(value, "engagementId");
        if (value.isBlank()) {
            throw new IllegalArgumentException("engagementId must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}

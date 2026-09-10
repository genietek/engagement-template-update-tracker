package com.caseware.templateupdate.model;

import java.util.Objects;

public record FirmId(String value) {
    public FirmId {
        Objects.requireNonNull(value, "firmId");
        if (value.isBlank()) {
            throw new IllegalArgumentException("firmId must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}

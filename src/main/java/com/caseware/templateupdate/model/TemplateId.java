package com.caseware.templateupdate.model;

import java.util.Objects;

public record TemplateId(String value) {
    public TemplateId {
        Objects.requireNonNull(value, "templateId");
        if (value.isBlank()) {
            throw new IllegalArgumentException("templateId must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}

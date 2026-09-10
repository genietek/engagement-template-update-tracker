package com.caseware.templateupdate.model;

public record TemplatePublished(TemplateId templateId, int version, String releaseNotes) {
    public TemplatePublished {
        if (version < 1) {
            throw new IllegalArgumentException("version must be >= 1");
        }
    }

    public TemplatePublished(TemplateId templateId, int version) {
        this(templateId, version, null);
    }
}

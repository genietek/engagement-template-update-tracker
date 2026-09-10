package com.caseware.templateupdate.model;

public record CreatedEngagement(
        EngagementId engagementId,
        FirmId firmId,
        TemplateId templateId,
        int version
) {
    public CreatedEngagement {
        if (version < 1) {
            throw new IllegalArgumentException("version must be >= 1");
        }
    }
}

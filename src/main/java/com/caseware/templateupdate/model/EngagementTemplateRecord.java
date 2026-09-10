package com.caseware.templateupdate.model;

public record EngagementTemplateRecord(
        EngagementId engagementId,
        FirmId firmId,
        TemplateId templateId,
        int appliedVersion,
        Integer dismissedThroughVersion
) {
    public EngagementTemplateRecord {
        if (appliedVersion < 1) {
            throw new IllegalArgumentException("appliedVersion must be >= 1");
        }
        if (dismissedThroughVersion != null && dismissedThroughVersion < 1) {
            throw new IllegalArgumentException("dismissedThroughVersion must be >= 1");
        }
    }

    public boolean isDismissedThrough(int version) {
        return dismissedThroughVersion != null && version <= dismissedThroughVersion;
    }
}

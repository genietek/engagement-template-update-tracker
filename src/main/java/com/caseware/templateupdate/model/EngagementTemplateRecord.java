package com.caseware.templateupdate.model;

/**
 * Projection row. {@code appliedVersion} is the template the engagement is
 * actually on (create, or last successful apply).
 *
 * <p>{@code dismissedThroughVersion} is how decline works without lying to the
 * user later. Declining v3 means "don't keep nagging me about v3." It does not
 * change {@code appliedVersion}. When v4 ships, pending reopens as applied→v4,
 * and that summary still includes v2/v3 content because applying v4 would.
 */
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

package com.caseware.templateupdate.model;

import java.util.List;

/**
 * {@code summary} is applied → latest (what apply would do right now).
 * {@code hops} is the same gap broken into consecutive publishes, for the
 * "three updates piled up while we were in the field" case.
 */
public record PendingUpdate(
        EngagementId engagementId,
        FirmId firmId,
        TemplateId templateId,
        int appliedVersion,
        int latestVersion,
        ChangeSummary summary,
        List<ChangeSummary> hops
) {
    public PendingUpdate {
        hops = List.copyOf(hops);
    }
}

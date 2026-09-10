package com.caseware.templateupdate.model;

import java.util.List;

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

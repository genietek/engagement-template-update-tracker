package com.caseware.templateupdate.model;

import java.util.List;

public record ChangeSummary(
        TemplateId templateId,
        int fromVersion,
        int toVersion,
        String headline,
        List<SummaryItem> items,
        String generator
) {
    public ChangeSummary {
        items = List.copyOf(items);
    }
}

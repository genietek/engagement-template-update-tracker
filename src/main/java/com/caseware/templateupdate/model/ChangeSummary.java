package com.caseware.templateupdate.model;

import java.util.List;

/**
 * Practitioner-facing changelog for one version span.
 * {@code generator} is {@code deterministic} in this slice; {@code llm} is
 * reserved so we can tell, in logs, which wording path produced a bullet.
 */
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

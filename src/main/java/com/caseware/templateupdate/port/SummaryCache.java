package com.caseware.templateupdate.port;

import com.caseware.templateupdate.model.ChangeSummary;
import com.caseware.templateupdate.model.TemplateId;

import java.util.Optional;

/**
 * Diff/summary cache keyed by {@code (templateId, from, to)}.
 * Template publishes are rare; identical gaps across a firm are common. This is
 * the difference between one JSON compare and hundreds.
 */
public interface SummaryCache {
    Optional<ChangeSummary> get(TemplateId templateId, int fromVersion, int toVersion);

    void put(ChangeSummary summary);
}

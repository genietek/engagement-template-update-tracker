package com.caseware.templateupdate.port;

import com.caseware.templateupdate.model.ChangeSummary;
import com.caseware.templateupdate.model.TemplateId;

import java.util.Optional;

public interface SummaryCache {
    Optional<ChangeSummary> get(TemplateId templateId, int fromVersion, int toVersion);

    void put(ChangeSummary summary);
}

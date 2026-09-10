package com.caseware.templateupdate.port;

import com.caseware.templateupdate.model.TemplateId;

import java.util.List;
import java.util.Optional;

public interface TemplateCatalog {
    void recordPublish(TemplateId templateId, int version);

    Optional<Integer> latestVersion(TemplateId templateId);

    /** Sorted ascending. */
    List<Integer> versions(TemplateId templateId);
}

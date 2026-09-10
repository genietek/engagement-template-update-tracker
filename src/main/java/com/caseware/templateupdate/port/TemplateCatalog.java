package com.caseware.templateupdate.port;

import com.caseware.templateupdate.model.TemplateId;

import java.util.List;
import java.util.Optional;

/**
 * Published versions of a product template. In production this is the shared
 * template database the assignment already describes. We only need version
 * numbers here — document bodies go through {@link TemplateDiffer}.
 */
public interface TemplateCatalog {
    void recordPublish(TemplateId templateId, int version);

    Optional<Integer> latestVersion(TemplateId templateId);

    /** Consecutive publishes, oldest first. Used to build the hop changelog. */
    List<Integer> versions(TemplateId templateId);
}

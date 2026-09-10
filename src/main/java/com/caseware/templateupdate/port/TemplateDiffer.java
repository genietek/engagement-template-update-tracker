package com.caseware.templateupdate.port;

import com.caseware.templateupdate.model.JsonDiff;
import com.caseware.templateupdate.model.TemplateId;

/**
 * Existing capability assumed by the assignment: fast, reliable JSON diff of two
 * template versions retrieved by (templateId, version).
 */
public interface TemplateDiffer {
    JsonDiff diff(TemplateId templateId, int fromVersion, int toVersion);
}

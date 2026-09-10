package com.caseware.templateupdate.port;

import com.caseware.templateupdate.model.JsonDiff;
import com.caseware.templateupdate.model.TemplateId;

/**
 * The assignment lets us assume template versions can be fetched quickly by
 * (id, version) and compared. Production would call the existing template
 * service; this port is that seam.
 *
 * <p>Callers must treat this as shared-template work, not per-engagement work.
 * Diff v1→v3 of audit-ifrs once, then attach the result to every file still on v1.
 */
public interface TemplateDiffer {
    JsonDiff diff(TemplateId templateId, int fromVersion, int toVersion);
}

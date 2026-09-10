package com.caseware.templateupdate.port;

import com.caseware.templateupdate.model.EngagementId;
import com.caseware.templateupdate.model.EngagementTemplateRecord;
import com.caseware.templateupdate.model.FirmId;
import com.caseware.templateupdate.model.TemplateId;

import java.util.List;
import java.util.Optional;

/**
 * The only place this slice is allowed to look up "what template version is
 * engagement X on?"
 *
 * <p>Engagement files live in per-firm databases and are not a queryable row of
 * current state — opening one rehydrates it into a session pod (~1 minute).
 * We therefore do not read engagements from here. The registry is filled by
 * hooks on create / apply / decline / open, which are moments the engagement
 * system already has the file in memory.
 *
 * <p>Metadata only: firm, engagement id, template id, applied version. No
 * financial fields. That is what makes a shared, cross-firm lookup acceptable
 * when a template publishes: "who is still on an older version of product P?"
 */
public interface EngagementTemplateRegistry {
    void upsert(EngagementTemplateRecord record);

    Optional<EngagementTemplateRecord> find(EngagementId engagementId);

    /**
     * Publish-time query. Implementations should be able to answer this without
     * visiting tenant engagement stores.
     */
    List<EngagementTemplateRecord> findOnTemplateOlderThan(TemplateId templateId, int version);

    List<EngagementTemplateRecord> listByFirm(FirmId firmId);
}

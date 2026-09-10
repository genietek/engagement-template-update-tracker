package com.caseware.templateupdate.port;

import com.caseware.templateupdate.model.EngagementId;
import com.caseware.templateupdate.model.EngagementTemplateRecord;
import com.caseware.templateupdate.model.FirmId;
import com.caseware.templateupdate.model.TemplateId;

import java.util.List;
import java.util.Optional;

/**
 * Queryable projection of engagement → template version.
 * Populated from engagement-management hooks. Never loads an engagement file.
 */
public interface EngagementTemplateRegistry {
    void upsert(EngagementTemplateRecord record);

    Optional<EngagementTemplateRecord> find(EngagementId engagementId);

    List<EngagementTemplateRecord> findOnTemplateOlderThan(TemplateId templateId, int version);

    List<EngagementTemplateRecord> listByFirm(FirmId firmId);
}

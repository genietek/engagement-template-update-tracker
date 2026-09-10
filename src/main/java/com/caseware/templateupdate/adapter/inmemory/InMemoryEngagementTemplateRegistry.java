package com.caseware.templateupdate.adapter.inmemory;

import com.caseware.templateupdate.model.EngagementId;
import com.caseware.templateupdate.model.EngagementTemplateRecord;
import com.caseware.templateupdate.model.FirmId;
import com.caseware.templateupdate.model.TemplateId;
import com.caseware.templateupdate.port.EngagementTemplateRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryEngagementTemplateRegistry implements EngagementTemplateRegistry {
    private final Map<EngagementId, EngagementTemplateRecord> records = new ConcurrentHashMap<>();

    @Override
    public void upsert(EngagementTemplateRecord record) {
        records.put(record.engagementId(), record);
    }

    @Override
    public Optional<EngagementTemplateRecord> find(EngagementId engagementId) {
        return Optional.ofNullable(records.get(engagementId));
    }

    @Override
    public List<EngagementTemplateRecord> findOnTemplateOlderThan(TemplateId templateId, int version) {
        List<EngagementTemplateRecord> result = new ArrayList<>();
        for (EngagementTemplateRecord record : records.values()) {
            if (record.templateId().equals(templateId) && record.appliedVersion() < version) {
                result.add(record);
            }
        }
        return result;
    }

    @Override
    public List<EngagementTemplateRecord> listByFirm(FirmId firmId) {
        List<EngagementTemplateRecord> result = new ArrayList<>();
        for (EngagementTemplateRecord record : records.values()) {
            if (record.firmId().equals(firmId)) {
                result.add(record);
            }
        }
        return result;
    }
}

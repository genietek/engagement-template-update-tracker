package com.caseware.templateupdate.adapter.inmemory;

import com.caseware.templateupdate.model.EngagementId;
import com.caseware.templateupdate.model.FirmId;
import com.caseware.templateupdate.model.PendingUpdate;
import com.caseware.templateupdate.port.PendingUpdateStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryPendingUpdateStore implements PendingUpdateStore {
    private final Map<EngagementId, PendingUpdate> updates = new ConcurrentHashMap<>();

    @Override
    public void put(PendingUpdate update) {
        updates.put(update.engagementId(), update);
    }

    @Override
    public void remove(EngagementId engagementId) {
        updates.remove(engagementId);
    }

    @Override
    public Optional<PendingUpdate> find(EngagementId engagementId) {
        return Optional.ofNullable(updates.get(engagementId));
    }

    @Override
    public List<PendingUpdate> listByFirm(FirmId firmId) {
        List<PendingUpdate> result = new ArrayList<>();
        for (PendingUpdate update : updates.values()) {
            if (update.firmId().equals(firmId)) {
                result.add(update);
            }
        }
        return result;
    }
}

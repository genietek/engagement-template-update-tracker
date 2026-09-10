package com.caseware.templateupdate.port;

import com.caseware.templateupdate.model.EngagementId;
import com.caseware.templateupdate.model.FirmId;
import com.caseware.templateupdate.model.PendingUpdate;

import java.util.List;
import java.util.Optional;

public interface PendingUpdateStore {
    void put(PendingUpdate update);

    void remove(EngagementId engagementId);

    Optional<PendingUpdate> find(EngagementId engagementId);

    List<PendingUpdate> listByFirm(FirmId firmId);
}

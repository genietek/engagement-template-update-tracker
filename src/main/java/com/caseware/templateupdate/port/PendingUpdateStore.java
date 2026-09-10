package com.caseware.templateupdate.port;

import com.caseware.templateupdate.model.EngagementId;
import com.caseware.templateupdate.model.FirmId;
import com.caseware.templateupdate.model.PendingUpdate;

import java.util.List;
import java.util.Optional;

/**
 * Materialized pending rows for the dashboard. One record per engagement, always
 * representing the current gap (applied → latest), not a queue of individual
 * publishes. Replacing the row on each publish is how stacked updates work.
 */
public interface PendingUpdateStore {
    void put(PendingUpdate update);

    void remove(EngagementId engagementId);

    Optional<PendingUpdate> find(EngagementId engagementId);

    List<PendingUpdate> listByFirm(FirmId firmId);
}

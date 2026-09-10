package com.caseware.templateupdate.model;

/**
 * Event from engagement management after it has processed apply/decline.
 *
 * <p>We take {@code targetVersion} from them instead of inferring it from our
 * pending store. Their system is the one that loaded the file. Ours might be
 * a few seconds behind if a publish raced the decision.
 */
public record DecisionEvent(EngagementId engagementId, Decision decision, int targetVersion) {
    public DecisionEvent {
        if (targetVersion < 1) {
            throw new IllegalArgumentException("targetVersion must be >= 1");
        }
    }
}

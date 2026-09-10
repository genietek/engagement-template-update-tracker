package com.caseware.templateupdate.model;

public record DecisionEvent(EngagementId engagementId, Decision decision, int targetVersion) {
    public DecisionEvent {
        if (targetVersion < 1) {
            throw new IllegalArgumentException("targetVersion must be >= 1");
        }
    }
}

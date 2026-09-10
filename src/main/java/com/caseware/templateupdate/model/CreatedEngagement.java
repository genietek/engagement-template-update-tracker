package com.caseware.templateupdate.model;

/**
 * Hook fired when engagement management creates a file. Version here is whatever
 * they actually stamped into the new file — usually latest, not always, if
 * publish and create race.
 */
public record CreatedEngagement(
        EngagementId engagementId,
        FirmId firmId,
        TemplateId templateId,
        int version
) {
    public CreatedEngagement {
        if (version < 1) {
            throw new IllegalArgumentException("version must be >= 1");
        }
    }
}

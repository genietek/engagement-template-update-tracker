package com.caseware.templateupdate.model;

public record AtAGlanceRow(
        EngagementId engagementId,
        TemplateId templateId,
        int appliedVersion,
        Integer latestVersion,
        boolean pending,
        String headline
) {}

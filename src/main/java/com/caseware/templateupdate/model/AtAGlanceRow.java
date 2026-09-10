package com.caseware.templateupdate.model;

/**
 * One row on the firm list. {@code pending} is the badge; {@code headline} is
 * the one-line reason to open the summary. Built from the registry + pending
 * store, never from a loaded engagement.
 */
public record AtAGlanceRow(
        EngagementId engagementId,
        TemplateId templateId,
        int appliedVersion,
        Integer latestVersion,
        boolean pending,
        String headline
) {}

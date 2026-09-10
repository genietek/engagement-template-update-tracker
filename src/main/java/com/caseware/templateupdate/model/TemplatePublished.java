package com.caseware.templateupdate.model;

/**
 * Shared-template event. Not tenant-specific — every firm using this product
 * may have files to notify. {@code releaseNotes} is optional copy from the
 * content team; when they bothered to write it, we use it as the headline.
 */
public record TemplatePublished(TemplateId templateId, int version, String releaseNotes) {
    public TemplatePublished {
        if (version < 1) {
            throw new IllegalArgumentException("version must be >= 1");
        }
    }

    public TemplatePublished(TemplateId templateId, int version) {
        this(templateId, version, null);
    }
}

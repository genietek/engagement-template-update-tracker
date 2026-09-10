package com.caseware.templateupdate.model;

import java.util.List;

/**
 * Raw structural delta between two template versions. Source of truth for
 * summaries. Wording can be rewritten; these operations cannot.
 */
public record JsonDiff(TemplateId templateId, int fromVersion, int toVersion, List<JsonDiffOp> operations) {
    public JsonDiff {
        operations = List.copyOf(operations);
        if (fromVersion < 1 || toVersion < 1) {
            throw new IllegalArgumentException("versions must be >= 1");
        }
        if (toVersion < fromVersion) {
            throw new IllegalArgumentException("toVersion must be >= fromVersion");
        }
    }
}

package com.caseware.templateupdate.adapter.inmemory;

import com.caseware.templateupdate.diff.JsonDiffer;
import com.caseware.templateupdate.model.JsonDiff;
import com.caseware.templateupdate.model.TemplateId;
import com.caseware.templateupdate.port.TemplateDiffer;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stand-in for the real template store + comparer. {@link #diffCalls()} exists
 * so tests can prove we did not run a JSON diff per engagement.
 */
public final class InMemoryTemplateDiffer implements TemplateDiffer {
    private final Map<String, JsonNode> documents = new ConcurrentHashMap<>();
    private final JsonDiffer jsonDiffer = new JsonDiffer();
    private final AtomicInteger diffCalls = new AtomicInteger();

    public void put(TemplateId templateId, int version, JsonNode document) {
        documents.put(key(templateId, version), document);
    }

    public int diffCalls() {
        return diffCalls.get();
    }

    @Override
    public JsonDiff diff(TemplateId templateId, int fromVersion, int toVersion) {
        diffCalls.incrementAndGet();
        JsonNode from = documents.get(key(templateId, fromVersion));
        JsonNode to = documents.get(key(templateId, toVersion));
        if (from == null || to == null) {
            throw new IllegalStateException(
                    "Missing template documents for " + templateId + " " + fromVersion + "→" + toVersion
            );
        }
        return jsonDiffer.diff(templateId, fromVersion, toVersion, from, to);
    }

    private static String key(TemplateId templateId, int version) {
        return templateId.value() + ":" + version;
    }
}

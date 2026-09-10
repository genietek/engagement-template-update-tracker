package com.caseware.templateupdate.adapter.inmemory;

import com.caseware.templateupdate.model.ChangeSummary;
import com.caseware.templateupdate.model.TemplateId;
import com.caseware.templateupdate.port.SummaryCache;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemorySummaryCache implements SummaryCache {
    private final Map<String, ChangeSummary> cache = new ConcurrentHashMap<>();

    @Override
    public Optional<ChangeSummary> get(TemplateId templateId, int fromVersion, int toVersion) {
        return Optional.ofNullable(cache.get(key(templateId, fromVersion, toVersion)));
    }

    @Override
    public void put(ChangeSummary summary) {
        cache.put(key(summary.templateId(), summary.fromVersion(), summary.toVersion()), summary);
    }

    public int size() {
        return cache.size();
    }

    private static String key(TemplateId templateId, int fromVersion, int toVersion) {
        return templateId.value() + ":" + fromVersion + ":" + toVersion;
    }
}

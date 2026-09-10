package com.caseware.templateupdate.app;

import com.caseware.templateupdate.ConflictException;
import com.caseware.templateupdate.NotFoundException;
import com.caseware.templateupdate.model.AtAGlanceRow;
import com.caseware.templateupdate.model.ChangeSummary;
import com.caseware.templateupdate.model.CreatedEngagement;
import com.caseware.templateupdate.model.Decision;
import com.caseware.templateupdate.model.DecisionEvent;
import com.caseware.templateupdate.model.EngagementId;
import com.caseware.templateupdate.model.EngagementTemplateRecord;
import com.caseware.templateupdate.model.FirmId;
import com.caseware.templateupdate.model.JsonDiff;
import com.caseware.templateupdate.model.PendingUpdate;
import com.caseware.templateupdate.model.TemplateId;
import com.caseware.templateupdate.model.TemplatePublished;
import com.caseware.templateupdate.port.EngagementTemplateRegistry;
import com.caseware.templateupdate.port.PendingUpdateStore;
import com.caseware.templateupdate.port.SummaryCache;
import com.caseware.templateupdate.port.SummaryGenerator;
import com.caseware.templateupdate.port.TemplateCatalog;
import com.caseware.templateupdate.port.TemplateDiffer;
import com.caseware.templateupdate.summary.DeterministicSummarizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Keeps a queryable engagement→template index and materializes human-readable
 * pending updates when templates are published.
 *
 * <p>This service has no engagement-loader dependency. The 1-minute engagement
 * load is never used to answer "what version is this file on?" or to list
 * pending updates.
 */
public final class PendingUpdateService {
    private final EngagementTemplateRegistry registry;
    private final TemplateCatalog catalog;
    private final TemplateDiffer differ;
    private final SummaryGenerator summarizer;
    private final PendingUpdateStore pending;
    private final SummaryCache cache;

    public PendingUpdateService(
            EngagementTemplateRegistry registry,
            TemplateCatalog catalog,
            TemplateDiffer differ,
            SummaryGenerator summarizer,
            PendingUpdateStore pending,
            SummaryCache cache
    ) {
        this.registry = Objects.requireNonNull(registry);
        this.catalog = Objects.requireNonNull(catalog);
        this.differ = Objects.requireNonNull(differ);
        this.summarizer = Objects.requireNonNull(summarizer);
        this.pending = Objects.requireNonNull(pending);
        this.cache = Objects.requireNonNull(cache);
    }

    public void onEngagementCreated(CreatedEngagement event) {
        EngagementTemplateRecord record = new EngagementTemplateRecord(
                event.engagementId(),
                event.firmId(),
                event.templateId(),
                event.version(),
                null
        );
        registry.upsert(record);
        syncPending(record);
    }

    public void onTemplatePublished(TemplatePublished event) {
        catalog.recordPublish(event.templateId(), event.version());
        List<EngagementTemplateRecord> affected =
                registry.findOnTemplateOlderThan(event.templateId(), event.version());

        Map<Integer, List<EngagementTemplateRecord>> byApplied = new LinkedHashMap<>();
        for (EngagementTemplateRecord record : affected) {
            byApplied.computeIfAbsent(record.appliedVersion(), key -> new ArrayList<>()).add(record);
        }

        for (Map.Entry<Integer, List<EngagementTemplateRecord>> group : byApplied.entrySet()) {
            Summaries summaries = buildSummaries(
                    event.templateId(),
                    group.getKey(),
                    event.version(),
                    event.releaseNotes()
            );
            for (EngagementTemplateRecord record : group.getValue()) {
                if (record.isDismissedThrough(event.version())) {
                    continue;
                }
                pending.put(new PendingUpdate(
                        record.engagementId(),
                        record.firmId(),
                        record.templateId(),
                        record.appliedVersion(),
                        event.version(),
                        summaries.accumulated(),
                        summaries.hops()
                ));
            }
        }
    }

    public void onDecision(DecisionEvent event) {
        EngagementTemplateRecord record = registry.find(event.engagementId())
                .orElseThrow(() -> new NotFoundException("engagement", event.engagementId().value()));

        EngagementTemplateRecord updated;
        if (event.decision() == Decision.APPLY) {
            if (event.targetVersion() < record.appliedVersion()) {
                throw new ConflictException(
                        "Cannot apply template v" + event.targetVersion()
                                + " over newer applied v" + record.appliedVersion()
                );
            }
            updated = new EngagementTemplateRecord(
                    record.engagementId(),
                    record.firmId(),
                    record.templateId(),
                    event.targetVersion(),
                    null
            );
        } else {
            updated = new EngagementTemplateRecord(
                    record.engagementId(),
                    record.firmId(),
                    record.templateId(),
                    record.appliedVersion(),
                    event.targetVersion()
            );
        }
        registry.upsert(updated);
        syncPending(updated);
    }

    public List<PendingUpdate> listPendingForFirm(FirmId firmId) {
        return pending.listByFirm(firmId);
    }

    public Optional<PendingUpdate> getPending(EngagementId engagementId) {
        return pending.find(engagementId);
    }

    public List<AtAGlanceRow> listAtAGlance(FirmId firmId) {
        List<AtAGlanceRow> rows = new ArrayList<>();
        for (EngagementTemplateRecord record : registry.listByFirm(firmId)) {
            Optional<PendingUpdate> maybePending = pending.find(record.engagementId());
            Integer latest = maybePending.map(PendingUpdate::latestVersion)
                    .orElseGet(() -> catalog.latestVersion(record.templateId()).orElse(null));
            rows.add(new AtAGlanceRow(
                    record.engagementId(),
                    record.templateId(),
                    record.appliedVersion(),
                    latest,
                    maybePending.isPresent(),
                    maybePending.map(p -> p.summary().headline()).orElse(null)
            ));
        }
        rows.sort(Comparator.comparing((AtAGlanceRow row) -> !row.pending())
                .thenComparing(row -> row.engagementId().value()));
        return rows;
    }

    private void syncPending(EngagementTemplateRecord record) {
        Optional<Integer> latest = catalog.latestVersion(record.templateId());
        if (latest.isEmpty() || latest.get() <= record.appliedVersion() || record.isDismissedThrough(latest.get())) {
            pending.remove(record.engagementId());
            return;
        }
        Summaries summaries = buildSummaries(record.templateId(), record.appliedVersion(), latest.get(), null);
        pending.put(new PendingUpdate(
                record.engagementId(),
                record.firmId(),
                record.templateId(),
                record.appliedVersion(),
                latest.get(),
                summaries.accumulated(),
                summaries.hops()
        ));
    }

    private Summaries buildSummaries(TemplateId templateId, int fromVersion, int toVersion, String releaseNotes) {
        ChangeSummary accumulated = summaryFor(templateId, fromVersion, toVersion, releaseNotes);
        List<Integer> chain = versionChain(templateId, fromVersion, toVersion);
        List<ChangeSummary> hops = new ArrayList<>();
        for (int i = 0; i < chain.size() - 1; i++) {
            hops.add(summaryFor(templateId, chain.get(i), chain.get(i + 1), null));
        }
        return new Summaries(accumulated, hops);
    }

    private List<Integer> versionChain(TemplateId templateId, int fromVersion, int toVersion) {
        List<Integer> chain = new ArrayList<>();
        for (int version : catalog.versions(templateId)) {
            if (version >= fromVersion && version <= toVersion) {
                chain.add(version);
            }
        }
        if (chain.isEmpty() || chain.getFirst() != fromVersion) {
            chain.addFirst(fromVersion);
        }
        if (chain.getLast() != toVersion) {
            chain.add(toVersion);
        }
        return chain;
    }

    private ChangeSummary summaryFor(TemplateId templateId, int fromVersion, int toVersion, String releaseNotes) {
        if (releaseNotes == null || releaseNotes.isBlank()) {
            Optional<ChangeSummary> cached = cache.get(templateId, fromVersion, toVersion);
            if (cached.isPresent()) {
                return cached.get();
            }
        }
        JsonDiff diff = differ.diff(templateId, fromVersion, toVersion);
        ChangeSummary summary = summarizer.summarize(diff, releaseNotes);
        DeterministicSummarizer.assertGrounded(diff, summary);
        cache.put(summary);
        return summary;
    }

    private record Summaries(ChangeSummary accumulated, List<ChangeSummary> hops) {}
}

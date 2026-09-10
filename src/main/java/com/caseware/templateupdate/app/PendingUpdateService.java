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
 * Core of the take-home slice.
 *
 * <p>The engagement system can tell you which template version a file is on, but only
 * after it rehydrates the file into a session pod. That load is ~1 minute and is a
 * hard constraint — we cannot scan a firm's engagements at publish time.
 *
 * <p>So this service never opens an engagement. It keeps a small projection
 * (engagement → applied template version) that is written from hooks the engagement
 * system already has in memory: create, apply, decline, and later ordinary open.
 * Template publishes are cheap (shared store, keyed by id+version). We join the two.
 *
 * <p>If several publishes land before the user decides, we do not stack a queue of
 * separate "please review v2", "please review v3" cards. The pending record is always
 * <em>what they are on → what latest is</em>, plus a hop list so they can see how
 * that gap grew week by week.
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

    /**
     * Engagement management already loaded the template to create the file, so this
     * is a free place to capture version. We still call {@link #syncPending} in case
     * the file was created from a slightly stale copy while a newer publish existed.
     */
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

    /**
     * Publishes are infrequent (~weekly per product) so we can do real work here.
     *
     * <p>Many firms share the same template, and many engagements inside a firm sit
     * on the same applied version. Diff + summary are computed once per
     * {@code (templateId, appliedVersion, newVersion)} and then copied onto each
     * affected row. Doing it per engagement would just recompute the same JSON.
     *
     * <p>Someone who already declined this exact target version is left alone; a
     * later publish (higher version) will re-open the decision.
     */
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

    /**
     * Apply/decline is processed by engagement management, which <em>does</em> load
     * the file (out of scope for us). By the time this event arrives, that work is
     * done — or at least committed — and we are told the version they acted on.
     *
     * <p>We still re-sync afterwards. The load can take a minute; a newer template
     * can publish in that window. If they applied v3 and v4 shipped while the pod
     * was busy, they should immediately see v3→v4 pending, not a blank dashboard.
     *
     * <p>Decline does not rewind the template and does not mean "never show me
     * these procedures again." It means stay on the current applied version and
     * dismiss this target. Accepting a later version will still bring those
     * changes in, so the next summary is honest about that.
     */
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

    /**
     * Firm dashboard: every engagement, pending ones first. This is a projection
     * read — if we had to open files to build it, the page would take hours.
     */
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

    /**
     * Single place that decides whether a row should show a pending update.
     * Used after create and after a decision so we do not duplicate the rules.
     */
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

    /**
     * {@code accumulated} is the practitioner view: everything that would land if
     * they apply latest right now. {@code hops} is the week-by-week changelog for
     * when two or three publishes stacked up before anyone looked.
     */
    private Summaries buildSummaries(TemplateId templateId, int fromVersion, int toVersion, String releaseNotes) {
        ChangeSummary accumulated = summaryFor(templateId, fromVersion, toVersion, releaseNotes);
        List<Integer> chain = versionChain(templateId, fromVersion, toVersion);
        List<ChangeSummary> hops = new ArrayList<>();
        for (int i = 0; i < chain.size() - 1; i++) {
            hops.add(summaryFor(templateId, chain.get(i), chain.get(i + 1), null));
        }
        return new Summaries(accumulated, hops);
    }

    /**
     * Catalog should already contain every published version, but create-hooks and
     * retries can race. Force the endpoints of the range onto the chain so we
     * still produce applied→latest even if an intermediate record is missing.
     */
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

    /**
     * Cache is keyed by version pair. A hundred files on v1 when v3 ships should
     * hit the differ once, not a hundred times. Release notes skip the cache on
     * the way in so a content-authored headline is not replaced by a stale
     * generated one (and then we store the result for everyone else).
     *
     * <p>Grounding runs on every generated summary, including an LLM adapter if
     * one is plugged in later. If a bullet cannot point at a real diff path, we
     * drop it on the floor rather than show practitioners invented methodology.
     */
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

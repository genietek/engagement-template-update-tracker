package com.caseware.templateupdate;

import com.caseware.templateupdate.adapter.inmemory.InMemoryPendingUpdateServiceFactory;
import com.caseware.templateupdate.app.PendingUpdateService;
import com.caseware.templateupdate.model.AtAGlanceRow;
import com.caseware.templateupdate.model.ChangeSummary;
import com.caseware.templateupdate.model.CreatedEngagement;
import com.caseware.templateupdate.model.Decision;
import com.caseware.templateupdate.model.DecisionEvent;
import com.caseware.templateupdate.model.EngagementId;
import com.caseware.templateupdate.model.FirmId;
import com.caseware.templateupdate.model.PendingUpdate;
import com.caseware.templateupdate.model.TemplateId;
import com.caseware.templateupdate.model.TemplatePublished;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PendingUpdateServiceTest {
    private static final TemplateId TEMPLATE = new TemplateId("audit-ifrs");
    private static final FirmId FIRM = new FirmId("firm-north");
    private static final EngagementId ALPHA = new EngagementId("eng-alpha");
    private static final EngagementId BETA = new EngagementId("eng-beta");
    private static final EngagementId GAMMA = new EngagementId("eng-gamma");

    private InMemoryPendingUpdateServiceFactory.Harness harness;
    private PendingUpdateService service;

    @BeforeEach
    void setUp() {
        harness = InMemoryPendingUpdateServiceFactory.harness();
        service = harness.service();
        harness.differ().put(TEMPLATE, 1, Templates.v1());
        harness.differ().put(TEMPLATE, 2, Templates.v2());
        harness.differ().put(TEMPLATE, 3, Templates.v3());
        harness.differ().put(TEMPLATE, 4, Templates.v4());
        service.onTemplatePublished(new TemplatePublished(TEMPLATE, 1));
    }

    @Test
    void createdOnLatestVersionHasNoPending() {
        service.onEngagementCreated(new CreatedEngagement(ALPHA, FIRM, TEMPLATE, 1));

        assertTrue(service.getPending(ALPHA).isEmpty());
        AtAGlanceRow row = glance(ALPHA);
        assertFalse(row.pending());
        assertEquals(1, row.appliedVersion());
    }

    @Test
    void publishMarksAllOlderEngagementsPendingWithoutLoadingThem() {
        service.onEngagementCreated(new CreatedEngagement(ALPHA, FIRM, TEMPLATE, 1));
        service.onEngagementCreated(new CreatedEngagement(BETA, FIRM, TEMPLATE, 1));

        service.onTemplatePublished(new TemplatePublished(TEMPLATE, 2, "Added going-concern evaluation procedure."));

        PendingUpdate alpha = service.getPending(ALPHA).orElseThrow();
        PendingUpdate beta = service.getPending(BETA).orElseThrow();
        assertEquals(1, alpha.appliedVersion());
        assertEquals(2, alpha.latestVersion());
        assertEquals("Added going-concern evaluation procedure.", alpha.summary().headline());
        assertEquals(beta.summary().items(), alpha.summary().items());
        assertTrue(alpha.summary().items().stream().anyMatch(item -> item.description().contains("Evaluate going concern")));
    }

    @Test
    void summariesAreComputedOncePerVersionPairNotPerEngagement() {
        service.onEngagementCreated(new CreatedEngagement(ALPHA, FIRM, TEMPLATE, 1));
        service.onEngagementCreated(new CreatedEngagement(BETA, FIRM, TEMPLATE, 1));
        service.onEngagementCreated(new CreatedEngagement(GAMMA, FIRM, TEMPLATE, 1));

        int before = harness.differ().diffCalls();
        service.onTemplatePublished(new TemplatePublished(TEMPLATE, 2));
        int after = harness.differ().diffCalls();

        assertEquals(1, after - before, "diff should run once for three engagements on the same applied version");
        assertEquals(3, service.listPendingForFirm(FIRM).size());
    }

    @Test
    void accumulatedPublishesRebuildSummaryFromAppliedToLatest() {
        service.onEngagementCreated(new CreatedEngagement(ALPHA, FIRM, TEMPLATE, 1));
        service.onTemplatePublished(new TemplatePublished(TEMPLATE, 2));
        service.onTemplatePublished(new TemplatePublished(TEMPLATE, 3));

        PendingUpdate pending = service.getPending(ALPHA).orElseThrow();
        assertEquals(1, pending.appliedVersion());
        assertEquals(3, pending.latestVersion());
        assertEquals(2, pending.hops().size());
        assertEquals(1, pending.hops().get(0).fromVersion());
        assertEquals(2, pending.hops().get(0).toVersion());
        assertEquals(2, pending.hops().get(1).fromVersion());
        assertEquals(3, pending.hops().get(1).toVersion());
        assertTrue(pending.summary().items().stream().anyMatch(i -> i.description().contains("Related-party")));
        assertTrue(pending.summary().items().stream().anyMatch(i -> i.description().contains("Evaluate going concern")));
    }

    @Test
    void applyUpdatesIndexAndClearsPendingWhenAlreadyOnLatest() {
        service.onEngagementCreated(new CreatedEngagement(ALPHA, FIRM, TEMPLATE, 1));
        service.onTemplatePublished(new TemplatePublished(TEMPLATE, 3));

        service.onDecision(new DecisionEvent(ALPHA, Decision.APPLY, 3));

        assertTrue(service.getPending(ALPHA).isEmpty());
        assertEquals(3, harness.registry().find(ALPHA).orElseThrow().appliedVersion());
        assertFalse(glance(ALPHA).pending());
    }

    @Test
    void applyResyncsIfANewerVersionWasPublishedDuringTheSlowLoad() {
        service.onEngagementCreated(new CreatedEngagement(ALPHA, FIRM, TEMPLATE, 1));
        service.onTemplatePublished(new TemplatePublished(TEMPLATE, 3));
        service.onTemplatePublished(new TemplatePublished(TEMPLATE, 4));

        service.onDecision(new DecisionEvent(ALPHA, Decision.APPLY, 3));

        PendingUpdate pending = service.getPending(ALPHA).orElseThrow();
        assertEquals(3, pending.appliedVersion());
        assertEquals(4, pending.latestVersion());
        assertTrue(pending.summary().items().stream().anyMatch(i -> i.description().contains("subsequent events")));
    }

    @Test
    void declineDismissesCurrentLatestButKeepsAppliedVersion() {
        service.onEngagementCreated(new CreatedEngagement(ALPHA, FIRM, TEMPLATE, 1));
        service.onTemplatePublished(new TemplatePublished(TEMPLATE, 3));

        service.onDecision(new DecisionEvent(ALPHA, Decision.DECLINE, 3));

        assertTrue(service.getPending(ALPHA).isEmpty());
        assertEquals(1, harness.registry().find(ALPHA).orElseThrow().appliedVersion());
        assertEquals(3, harness.registry().find(ALPHA).orElseThrow().dismissedThroughVersion());
    }

    @Test
    void laterPublishAfterDeclineReopensPendingFromOriginalAppliedVersion() {
        service.onEngagementCreated(new CreatedEngagement(ALPHA, FIRM, TEMPLATE, 1));
        service.onTemplatePublished(new TemplatePublished(TEMPLATE, 3));
        service.onDecision(new DecisionEvent(ALPHA, Decision.DECLINE, 3));

        service.onTemplatePublished(new TemplatePublished(TEMPLATE, 4));

        PendingUpdate pending = service.getPending(ALPHA).orElseThrow();
        assertEquals(1, pending.appliedVersion());
        assertEquals(4, pending.latestVersion());
        assertTrue(pending.summary().items().stream().anyMatch(i -> i.description().contains("Evaluate going concern")));
        assertTrue(pending.summary().items().stream().anyMatch(i -> i.description().contains("subsequent events")));
    }

    @Test
    void atAGlanceListsPendingFirst() {
        service.onEngagementCreated(new CreatedEngagement(ALPHA, FIRM, TEMPLATE, 1));
        service.onEngagementCreated(new CreatedEngagement(BETA, FIRM, TEMPLATE, 1));
        service.onTemplatePublished(new TemplatePublished(TEMPLATE, 2));
        service.onDecision(new DecisionEvent(ALPHA, Decision.APPLY, 2));

        List<AtAGlanceRow> rows = service.listAtAGlance(FIRM);
        assertEquals(BETA, rows.get(0).engagementId());
        assertTrue(rows.get(0).pending());
        assertEquals(ALPHA, rows.get(1).engagementId());
        assertFalse(rows.get(1).pending());
    }

    @Test
    void everySummaryItemIsGroundedInTheJsonDiff() {
        service.onEngagementCreated(new CreatedEngagement(ALPHA, FIRM, TEMPLATE, 1));
        service.onTemplatePublished(new TemplatePublished(TEMPLATE, 3));

        ChangeSummary summary = service.getPending(ALPHA).orElseThrow().summary();
        assertFalse(summary.items().isEmpty());
        summary.items().forEach(item -> assertFalse(item.sourcePath().isBlank()));
    }

    @Test
    void unknownEngagementDecisionFails() {
        assertThrows(NotFoundException.class, () ->
                service.onDecision(new DecisionEvent(ALPHA, Decision.APPLY, 2)));
    }

    @Test
    void cannotApplyOlderVersionThanCurrentlyApplied() {
        service.onEngagementCreated(new CreatedEngagement(ALPHA, FIRM, TEMPLATE, 1));
        service.onTemplatePublished(new TemplatePublished(TEMPLATE, 3));
        service.onDecision(new DecisionEvent(ALPHA, Decision.APPLY, 3));

        assertThrows(ConflictException.class, () ->
                service.onDecision(new DecisionEvent(ALPHA, Decision.APPLY, 2)));
    }

    private AtAGlanceRow glance(EngagementId id) {
        return service.listAtAGlance(FIRM).stream()
                .filter(row -> row.engagementId().equals(id))
                .findFirst()
                .orElseThrow();
    }
}

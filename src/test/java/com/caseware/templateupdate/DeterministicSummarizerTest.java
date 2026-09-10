package com.caseware.templateupdate;

import com.caseware.templateupdate.diff.JsonDiffer;
import com.caseware.templateupdate.model.ChangeAction;
import com.caseware.templateupdate.model.ChangeSummary;
import com.caseware.templateupdate.model.JsonDiff;
import com.caseware.templateupdate.model.JsonDiffOp;
import com.caseware.templateupdate.model.SummaryItem;
import com.caseware.templateupdate.model.TemplateId;
import com.caseware.templateupdate.summary.DeterministicSummarizer;
import com.caseware.templateupdate.summary.UngroundedSummaryException;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeterministicSummarizerTest {
    private final DeterministicSummarizer summarizer = new DeterministicSummarizer();
    private final JsonDiffer differ = new JsonDiffer();
    private final TemplateId template = new TemplateId("audit-ifrs");

    @Test
    void prefersContentAuthoredReleaseNotesForHeadline() {
        JsonDiff diff = differ.diff(template, 1, 2, Templates.v1(), Templates.v2());
        ChangeSummary summary = summarizer.summarize(diff, "Added going-concern evaluation procedure.");
        assertEquals("Added going-concern evaluation procedure.", summary.headline());
        assertEquals("deterministic", summary.generator());
    }

    @Test
    void describesAddedEntitiesInPractitionerLanguage() {
        JsonDiff diff = differ.diff(template, 1, 2, Templates.v1(), Templates.v2());
        ChangeSummary summary = summarizer.summarize(diff, null);
        assertTrue(summary.items().stream().anyMatch(item ->
                item.action() == ChangeAction.ADDED
                        && item.category().equals("Procedures")
                        && item.description().contains("Evaluate going concern")
                        && item.sourcePath().equals("/procedures/P-210")));
    }

    @Test
    void describesNestedFieldAddsAsUpdatesOnTheExistingItem() {
        JsonDiff diff = differ.diff(template, 2, 3, Templates.v2(), Templates.v3());
        ChangeSummary summary = summarizer.summarize(diff, null);
        assertTrue(summary.items().stream().anyMatch(item ->
                item.action() == ChangeAction.UPDATED
                        && item.description().contains("P-100")
                        && item.description().contains("guidance")
                        && item.description().contains("compensating balances")));
        assertTrue(summary.items().stream().anyMatch(item ->
                item.action() == ChangeAction.ADDED
                        && item.description().contains("Related-party transactions")));
    }

    @Test
    void rejectHallucinatedSummaryPaths() {
        JsonDiff diff = differ.diff(template, 1, 2, Templates.v1(), Templates.v2());
        ChangeSummary forged = new ChangeSummary(
                template,
                1,
                2,
                "Invented change",
                List.of(new SummaryItem("/does-not-exist", ChangeAction.ADDED, "Procedures", "Added a fake procedure")),
                "llm"
        );
        assertThrows(UngroundedSummaryException.class, () -> DeterministicSummarizer.assertGrounded(diff, forged));
    }

    @Test
    void groundedItemsPass() {
        JsonDiff diff = new JsonDiff(template, 1, 2, List.of(
                new JsonDiffOp.Add("/procedures/P-210", TextNode.valueOf("x"))
        ));
        ChangeSummary summary = summarizer.summarize(diff, null);
        DeterministicSummarizer.assertGrounded(diff, summary);
        assertEquals(1, summary.items().size());
    }
}

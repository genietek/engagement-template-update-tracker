package com.caseware.templateupdate.demo;

import com.caseware.templateupdate.adapter.inmemory.InMemoryPendingUpdateServiceFactory;
import com.caseware.templateupdate.adapter.inmemory.InMemoryTemplateDiffer;
import com.caseware.templateupdate.app.PendingUpdateService;
import com.caseware.templateupdate.model.AtAGlanceRow;
import com.caseware.templateupdate.model.CreatedEngagement;
import com.caseware.templateupdate.model.Decision;
import com.caseware.templateupdate.model.DecisionEvent;
import com.caseware.templateupdate.model.EngagementId;
import com.caseware.templateupdate.model.FirmId;
import com.caseware.templateupdate.model.PendingUpdate;
import com.caseware.templateupdate.model.SummaryItem;
import com.caseware.templateupdate.model.TemplateId;
import com.caseware.templateupdate.model.TemplatePublished;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class DemoApp {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TemplateId TEMPLATE = new TemplateId("audit-ifrs");
    private static final FirmId FIRM = new FirmId("firm-north");

    public static void main(String[] args) {
        InMemoryTemplateDiffer differ = new InMemoryTemplateDiffer();
        differ.put(TEMPLATE, 1, v1());
        differ.put(TEMPLATE, 2, v2());
        differ.put(TEMPLATE, 3, v3());
        differ.put(TEMPLATE, 4, v4());

        PendingUpdateService service = InMemoryPendingUpdateServiceFactory.create(differ);

        service.onTemplatePublished(new TemplatePublished(TEMPLATE, 1));
        service.onEngagementCreated(new CreatedEngagement(new EngagementId("eng-alpha"), FIRM, TEMPLATE, 1));
        service.onEngagementCreated(new CreatedEngagement(new EngagementId("eng-beta"), FIRM, TEMPLATE, 1));
        service.onEngagementCreated(new CreatedEngagement(new EngagementId("eng-gamma"), FIRM, TEMPLATE, 1));

        printGlance("After creation on v1", service);
        service.onTemplatePublished(new TemplatePublished(
                TEMPLATE, 2, "Added going-concern evaluation procedure."
        ));
        printGlance("After publish v2", service);
        printDetail(service, "eng-alpha");

        service.onTemplatePublished(new TemplatePublished(TEMPLATE, 3));
        printGlance("After publish v3 (accumulated before any decision)", service);
        printDetail(service, "eng-alpha");

        service.onDecision(new DecisionEvent(new EngagementId("eng-alpha"), Decision.APPLY, 3));
        service.onDecision(new DecisionEvent(new EngagementId("eng-beta"), Decision.DECLINE, 3));
        printGlance("Alpha applied v3, Beta declined v3, Gamma still pending", service);

        service.onTemplatePublished(new TemplatePublished(TEMPLATE, 4));
        printGlance("After publish v4", service);
        printDetail(service, "eng-alpha");
        printDetail(service, "eng-beta");
        printDetail(service, "eng-gamma");

        System.out.println();
        System.out.println("JSON diffs computed: " + differ.diffCalls()
                + " (shared across engagements via per-version-pair cache)");
    }

    private static void printGlance(String title, PendingUpdateService service) {
        System.out.println();
        System.out.println("=== " + title + " ===");
        for (AtAGlanceRow row : service.listAtAGlance(FIRM)) {
            String badge = row.pending() ? "PENDING" : "current";
            String latest = row.latestVersion() == null ? "-" : "v" + row.latestVersion();
            String headline = row.headline() == null ? "" : " | " + row.headline();
            System.out.println("  " + row.engagementId() + "  " + badge
                    + "  applied v" + row.appliedVersion()
                    + "  latest " + latest + headline);
        }
    }

    private static void printDetail(PendingUpdateService service, String engagement) {
        EngagementId id = new EngagementId(engagement);
        PendingUpdate pending = service.getPending(id).orElse(null);
        if (pending == null) {
            System.out.println();
            System.out.println(engagement + ": no pending update");
            return;
        }
        System.out.println();
        System.out.println(engagement + " summary (v" + pending.appliedVersion()
                + " → v" + pending.latestVersion() + "): " + pending.summary().headline());
        for (SummaryItem item : pending.summary().items()) {
            System.out.println("  - [" + item.category() + "] " + item.description());
        }
        if (pending.hops().size() > 1) {
            System.out.println("  hops:");
            pending.hops().forEach(hop ->
                    System.out.println("    v" + hop.fromVersion() + "→v" + hop.toVersion() + ": " + hop.headline()));
        }
    }

    private static JsonNode v1() {
        ObjectNode root = JSON.createObjectNode();
        root.set("procedures", array(procedure("P-100", "Confirm cash balances", true, null)));
        root.set("checklists", array(item("C-10", "Year-end close checklist")));
        return root;
    }

    private static JsonNode v2() {
        ObjectNode root = JSON.createObjectNode();
        root.set("procedures", array(
                procedure("P-100", "Confirm cash balances", true, null),
                procedure("P-210", "Evaluate going concern", true, null)
        ));
        root.set("checklists", array(item("C-10", "Year-end close checklist")));
        return root;
    }

    private static JsonNode v3() {
        ObjectNode root = JSON.createObjectNode();
        root.set("procedures", array(
                procedure("P-100", "Confirm cash balances", true, "Include compensating balances."),
                procedure("P-210", "Evaluate going concern", true, null)
        ));
        root.set("checklists", array(item("C-10", "Year-end close checklist")));
        root.set("disclosures", array(item("D-3", "Related-party transactions")));
        return root;
    }

    private static JsonNode v4() {
        ObjectNode root = (ObjectNode) v3();
        ArrayNode procedures = (ArrayNode) root.get("procedures");
        procedures.add(procedure("P-300", "Test subsequent events", false, null));
        return root;
    }

    private static ArrayNode array(ObjectNode... nodes) {
        ArrayNode array = JSON.createArrayNode();
        for (ObjectNode node : nodes) {
            array.add(node);
        }
        return array;
    }

    private static ObjectNode item(String id, String title) {
        ObjectNode node = JSON.createObjectNode();
        node.put("id", id);
        node.put("title", title);
        return node;
    }

    private static ObjectNode procedure(String id, String title, boolean required, String guidance) {
        ObjectNode node = item(id, title);
        node.put("required", required);
        if (guidance != null) {
            node.put("guidance", guidance);
        }
        return node;
    }
}

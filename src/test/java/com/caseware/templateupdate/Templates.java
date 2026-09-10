package com.caseware.templateupdate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

final class Templates {
    static final ObjectMapper JSON = new ObjectMapper();

    private Templates() {}

    static JsonNode v1() {
        ObjectNode root = JSON.createObjectNode();
        root.set("procedures", array(procedure("P-100", "Confirm cash balances", true, null)));
        root.set("checklists", array(item("C-10", "Year-end close checklist")));
        return root;
    }

    static JsonNode v2() {
        ObjectNode root = JSON.createObjectNode();
        root.set("procedures", array(
                procedure("P-100", "Confirm cash balances", true, null),
                procedure("P-210", "Evaluate going concern", true, null)
        ));
        root.set("checklists", array(item("C-10", "Year-end close checklist")));
        return root;
    }

    static JsonNode v3() {
        ObjectNode root = JSON.createObjectNode();
        root.set("procedures", array(
                procedure("P-100", "Confirm cash balances", true, "Include compensating balances."),
                procedure("P-210", "Evaluate going concern", true, null)
        ));
        root.set("checklists", array(item("C-10", "Year-end close checklist")));
        root.set("disclosures", array(item("D-3", "Related-party transactions")));
        return root;
    }

    static JsonNode v4() {
        ObjectNode root = v3().deepCopy();
        ((ArrayNode) root.get("procedures")).add(procedure("P-300", "Test subsequent events", false, null));
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

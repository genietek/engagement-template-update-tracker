package com.caseware.templateupdate.diff;

import com.caseware.templateupdate.model.JsonDiff;
import com.caseware.templateupdate.model.JsonDiffOp;
import com.caseware.templateupdate.model.TemplateId;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Small structural differ for the slice. Production can swap this out for the
 * "quick and reliable" comparer the assignment says already exists.
 *
 * <p>Audit templates are mostly lists of entities with stable {@code id}s
 * (procedures, checklists, disclosures). Matching those arrays by id — not by
 * index — is what keeps "we inserted P-210 at the top" from showing up as a
 * rewrite of every later item. If an array has no ids (or duplicate ids) we
 * fall back to index comparison and live with noisier output.
 *
 * <p>Paths are JSON-pointer-ish so summary grounding has a stable string to
 * check. Users never see the pointer; they see the summarizer's sentence.
 */
public final class JsonDiffer {
    public JsonDiff diff(TemplateId templateId, int fromVersion, int toVersion, JsonNode from, JsonNode to) {
        List<JsonDiffOp> operations = new ArrayList<>();
        diffValues(from, to, "", operations);
        return new JsonDiff(templateId, fromVersion, toVersion, operations);
    }

    private void diffValues(JsonNode from, JsonNode to, String path, List<JsonDiffOp> operations) {
        if (from.equals(to)) {
            return;
        }
        if (from instanceof ObjectNode fromObj && to instanceof ObjectNode toObj) {
            diffObjects(fromObj, toObj, path, operations);
            return;
        }
        if (from instanceof ArrayNode fromArr && to instanceof ArrayNode toArr) {
            diffArrays(fromArr, toArr, path, operations);
            return;
        }
        operations.add(new JsonDiffOp.Replace(path.isEmpty() ? "/" : path, from, to));
    }

    private void diffObjects(ObjectNode from, ObjectNode to, String path, List<JsonDiffOp> operations) {
        Set<String> names = new LinkedHashSet<>();
        from.fieldNames().forEachRemaining(names::add);
        to.fieldNames().forEachRemaining(names::add);
        for (String name : names) {
            String child = pointer(path, name);
            boolean hasFrom = from.has(name);
            boolean hasTo = to.has(name);
            if (!hasFrom) {
                operations.add(new JsonDiffOp.Add(child, to.get(name)));
            } else if (!hasTo) {
                operations.add(new JsonDiffOp.Remove(child, from.get(name)));
            } else {
                diffValues(from.get(name), to.get(name), child, operations);
            }
        }
    }

    /**
     * Prefer identity over position. Content teams reorder procedures without
     * meaning to "change" them; index diffs would scream about every move.
     */
    private void diffArrays(ArrayNode from, ArrayNode to, String path, List<JsonDiffOp> operations) {
        Map<String, JsonNode> fromById = keyed(from);
        Map<String, JsonNode> toById = keyed(to);
        if (fromById != null && toById != null) {
            for (Map.Entry<String, JsonNode> entry : fromById.entrySet()) {
                if (!toById.containsKey(entry.getKey())) {
                    operations.add(new JsonDiffOp.Remove(pointer(path, entry.getKey()), entry.getValue()));
                }
            }
            for (Map.Entry<String, JsonNode> entry : toById.entrySet()) {
                if (!fromById.containsKey(entry.getKey())) {
                    operations.add(new JsonDiffOp.Add(pointer(path, entry.getKey()), entry.getValue()));
                } else {
                    diffValues(fromById.get(entry.getKey()), entry.getValue(), pointer(path, entry.getKey()), operations);
                }
            }
            return;
        }

        int max = Math.max(from.size(), to.size());
        for (int i = 0; i < max; i++) {
            String child = pointer(path, Integer.toString(i));
            if (i >= from.size()) {
                operations.add(new JsonDiffOp.Add(child, to.get(i)));
            } else if (i >= to.size()) {
                operations.add(new JsonDiffOp.Remove(child, from.get(i)));
            } else {
                diffValues(from.get(i), to.get(i), child, operations);
            }
        }
    }

    /** {@code null} means "this array is not safely keyable — use indexes." */
    private static Map<String, JsonNode> keyed(ArrayNode array) {
        Map<String, JsonNode> byId = new LinkedHashMap<>();
        for (JsonNode element : array) {
            if (!element.has("id") || element.get("id").isNull()) {
                return null;
            }
            JsonNode idNode = element.get("id");
            String id = idNode.isNumber() ? idNode.numberValue().toString() : idNode.asText();
            if (id.isBlank() || byId.containsKey(id)) {
                return null;
            }
            byId.put(id, element);
        }
        return byId;
    }

    private static String pointer(String parent, String key) {
        String escaped = key.replace("~", "~0").replace("/", "~1");
        return parent + "/" + escaped;
    }
}

package com.caseware.templateupdate.summary;

import com.caseware.templateupdate.model.ChangeAction;
import com.caseware.templateupdate.model.ChangeSummary;
import com.caseware.templateupdate.model.JsonDiff;
import com.caseware.templateupdate.model.JsonDiffOp;
import com.caseware.templateupdate.model.SummaryItem;
import com.caseware.templateupdate.port.SummaryGenerator;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class DeterministicSummarizer implements SummaryGenerator {
    private static final Map<String, String> CATEGORIES = Map.of(
            "procedures", "Procedures",
            "checklists", "Checklists",
            "disclosures", "Disclosures",
            "guidance", "Guidance",
            "workpapers", "Workpapers",
            "methodology", "Methodology"
    );

    @Override
    public ChangeSummary summarize(JsonDiff diff, String releaseNotes) {
        List<SummaryItem> items = new ArrayList<>();
        for (JsonDiffOp op : diff.operations()) {
            items.add(toItem(op));
        }
        return new ChangeSummary(
                diff.templateId(),
                diff.fromVersion(),
                diff.toVersion(),
                headline(diff, items, releaseNotes),
                items,
                "deterministic"
        );
    }

    public static void assertGrounded(JsonDiff diff, ChangeSummary summary) {
        Set<String> allowed = diff.operations().stream().map(JsonDiffOp::path).collect(Collectors.toSet());
        for (SummaryItem item : summary.items()) {
            if (!allowed.contains(item.sourcePath())) {
                throw new UngroundedSummaryException(
                        "Ungrounded summary item for path " + item.sourcePath()
                );
            }
        }
    }

    private static SummaryItem toItem(JsonDiffOp op) {
        return switch (op) {
            case JsonDiffOp.Add add -> describeAdd(add);
            case JsonDiffOp.Remove remove -> new SummaryItem(
                    remove.path(),
                    ChangeAction.REMOVED,
                    category(remove.path()),
                    "Removed " + singular(category(remove.path())) + " " + label(remove.value())
            );
            case JsonDiffOp.Replace replace -> new SummaryItem(
                    replace.path(),
                    ChangeAction.UPDATED,
                    category(replace.path()),
                    "Updated " + leaf(replace.path()) + ": " + compact(replace.from()) + " → " + compact(replace.to())
            );
        };
    }

    private static SummaryItem describeAdd(JsonDiffOp.Add add) {
        JsonNode value = add.value();
        if (isNestedScalarField(add.path(), value)) {
            return new SummaryItem(
                    add.path(),
                    ChangeAction.UPDATED,
                    category(add.path()),
                    "Updated " + parentEntity(add.path()) + ": added " + leaf(add.path())
                            + " \"" + compact(value) + "\""
            );
        }
        return new SummaryItem(
                add.path(),
                ChangeAction.ADDED,
                category(add.path()),
                "Added " + singular(category(add.path())) + " " + label(value)
        );
    }

    private static boolean isNestedScalarField(String path, JsonNode value) {
        return segments(path).size() >= 3 && (value == null || !value.isContainerNode() || !value.has("title"));
    }

    private static String parentEntity(String path) {
        List<String> parts = segments(path);
        if (parts.size() >= 2) {
            return parts.get(parts.size() - 2);
        }
        return leaf(path);
    }

    private static List<String> segments(String path) {
        List<String> parts = new ArrayList<>();
        for (String part : path.split("/")) {
            if (!part.isBlank()) {
                parts.add(unescape(part));
            }
        }
        return parts;
    }

    private static String headline(JsonDiff diff, List<SummaryItem> items, String releaseNotes) {
        if (releaseNotes != null && !releaseNotes.isBlank()) {
            return releaseNotes.trim();
        }
        if (items.isEmpty()) {
            return "Template " + diff.templateId() + " v" + diff.fromVersion() + " → v" + diff.toVersion()
                    + ": no content changes";
        }
        long added = items.stream().filter(i -> i.action() == ChangeAction.ADDED).count();
        long removed = items.stream().filter(i -> i.action() == ChangeAction.REMOVED).count();
        long updated = items.stream().filter(i -> i.action() == ChangeAction.UPDATED).count();
        List<String> parts = new ArrayList<>();
        if (added > 0) {
            parts.add(added + " added");
        }
        if (removed > 0) {
            parts.add(removed + " removed");
        }
        if (updated > 0) {
            parts.add(updated + " updated");
        }
        return "Template " + diff.templateId() + " v" + diff.fromVersion() + " → v" + diff.toVersion()
                + ": " + String.join(", ", parts);
    }

    private static String category(String path) {
        String first = firstSegment(path);
        if (first == null) {
            return "Template";
        }
        return CATEGORIES.getOrDefault(first, capitalize(first));
    }

    private static String singular(String category) {
        if (category.endsWith("s") && category.length() > 1) {
            return category.substring(0, category.length() - 1).toLowerCase();
        }
        return category.toLowerCase();
    }

    private static String label(JsonNode value) {
        if (value != null && value.isArray() && !value.isEmpty()) {
            List<String> parts = new ArrayList<>();
            for (JsonNode element : value) {
                parts.add(label(element));
            }
            return String.join("; ", parts);
        }
        if (value != null && value.hasNonNull("title")) {
            String title = value.get("title").asText();
            if (value.hasNonNull("id")) {
                return "\"" + title + "\" (" + value.get("id").asText() + ")";
            }
            return "\"" + title + "\"";
        }
        return compact(value);
    }

    private static String compact(JsonNode value) {
        if (value == null || value.isNull()) {
            return "null";
        }
        if (value.isTextual() || value.isNumber() || value.isBoolean()) {
            return value.asText();
        }
        if (value.hasNonNull("title")) {
            return label(value);
        }
        String json = value.toString();
        return json.length() > 80 ? json.substring(0, 77) + "..." : json;
    }

    private static String firstSegment(String path) {
        String[] parts = path.split("/");
        for (String part : parts) {
            if (!part.isBlank()) {
                return unescape(part);
            }
        }
        return null;
    }

    private static String leaf(String path) {
        String[] parts = path.split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            if (!parts[i].isBlank()) {
                return unescape(parts[i]);
            }
        }
        return "item";
    }

    private static String unescape(String segment) {
        return segment.replace("~1", "/").replace("~0", "~");
    }

    private static String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}

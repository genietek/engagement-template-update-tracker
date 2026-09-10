package com.caseware.templateupdate.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Structural change against two template documents. {@code path} is the join
 * key for grounding; keep it stable (JSON pointer style) even if wording changes.
 */
public sealed interface JsonDiffOp {
    String path();

    record Add(String path, JsonNode value) implements JsonDiffOp {}

    record Remove(String path, JsonNode value) implements JsonDiffOp {}

    record Replace(String path, JsonNode from, JsonNode to) implements JsonDiffOp {}
}

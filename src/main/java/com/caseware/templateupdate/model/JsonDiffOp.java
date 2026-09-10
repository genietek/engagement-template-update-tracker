package com.caseware.templateupdate.model;

import com.fasterxml.jackson.databind.JsonNode;

public sealed interface JsonDiffOp {
    String path();

    record Add(String path, JsonNode value) implements JsonDiffOp {}

    record Remove(String path, JsonNode value) implements JsonDiffOp {}

    record Replace(String path, JsonNode from, JsonNode to) implements JsonDiffOp {}
}

package com.caseware.templateupdate.model;

/**
 * One human-readable bullet. {@code sourcePath} is not shown in the UI; it is
 * the grounding handle. If an LLM writes a description, it still has to point
 * this at a path from the JSON diff or the service will reject the summary.
 */
public record SummaryItem(String sourcePath, ChangeAction action, String category, String description) {}

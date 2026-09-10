package com.caseware.templateupdate.model;

public record SummaryItem(String sourcePath, ChangeAction action, String category, String description) {}

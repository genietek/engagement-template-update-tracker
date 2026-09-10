package com.caseware.templateupdate.port;

import com.caseware.templateupdate.model.ChangeSummary;
import com.caseware.templateupdate.model.JsonDiff;

/**
 * Turns a JSON diff into something a non-technical practitioner can read.
 *
 * <p>Content-authored {@code releaseNotes} win as the headline when present —
 * the content team already knows what they shipped. Generated bullets still
 * come from the diff so the detail view stays tied to actual template changes.
 *
 * <p>An LLM implementation is allowed later (Bedrock, etc.) but it must still
 * put a real diff path on every {@code SummaryItem}. The service grounding
 * check will reject hallucinated procedures. Do not use this port to recommend
 * apply vs decline; that is professional judgment.
 */
public interface SummaryGenerator {
    ChangeSummary summarize(JsonDiff diff, String releaseNotes);
}

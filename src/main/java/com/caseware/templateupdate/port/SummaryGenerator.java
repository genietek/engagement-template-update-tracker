package com.caseware.templateupdate.port;

import com.caseware.templateupdate.model.ChangeSummary;
import com.caseware.templateupdate.model.JsonDiff;

public interface SummaryGenerator {
    ChangeSummary summarize(JsonDiff diff, String releaseNotes);
}

package com.caseware.templateupdate.model;

/**
 * APPLY means engagement management finished putting that template version onto
 * the file. DECLINE means they kept the current applied version and dismissed
 * this target. Neither path loads the file inside this slice.
 */
public enum Decision {
    APPLY,
    DECLINE
}

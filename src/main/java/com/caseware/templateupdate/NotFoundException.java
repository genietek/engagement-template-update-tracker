package com.caseware.templateupdate;

public class NotFoundException extends DomainException {
    public NotFoundException(String what, String id) {
        super("not_found", what + " not found: " + id);
    }
}

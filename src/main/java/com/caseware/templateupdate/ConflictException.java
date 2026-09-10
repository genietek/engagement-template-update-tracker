package com.caseware.templateupdate;

public class ConflictException extends DomainException {
    public ConflictException(String message) {
        super("conflict", message);
    }
}

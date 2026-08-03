package com.benhsoan.domain.visit.exception;

import org.springframework.http.HttpStatus;

import com.benhsoan.domain.shared.exception.DomainException;

public class VisitEncounterAccessDeniedException extends DomainException {

    public VisitEncounterAccessDeniedException() {
        super(HttpStatus.FORBIDDEN, "You do not have permission to view this visit encounter.");
    }
}

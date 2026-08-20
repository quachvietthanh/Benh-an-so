package com.benhsoan.domain.visit.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import java.util.UUID;


public class VisitNotFoundException extends VisitException {

    public VisitNotFoundException(UUID visitId) {
        super(DomainErrorCode.VISIT_NOT_FOUND, "Visit not found: " + visitId);
    }
}

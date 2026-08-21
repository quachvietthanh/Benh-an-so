package com.benhsoan.domain.visit.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class VisitInvalidStatusException extends VisitException {

    public VisitInvalidStatusException(String message) {
        super(DomainErrorCode.VISIT_INVALID_STATUS, message);
    }
}

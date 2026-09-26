package com.benhsoan.domain.visit.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class VisitAlreadyCancelledException extends VisitException {

    public VisitAlreadyCancelledException() {
        super(DomainErrorCode.VISIT_ALREADY_CANCELLED, "Visit has already been cancelled.");
    }
}

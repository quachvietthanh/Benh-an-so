package com.benhsoan.domain.visit.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class VisitAlreadyCompletedException extends VisitException {

    public VisitAlreadyCompletedException() {
        super(DomainErrorCode.VISIT_ALREADY_COMPLETED, "Visit has already been completed.");
    }
}

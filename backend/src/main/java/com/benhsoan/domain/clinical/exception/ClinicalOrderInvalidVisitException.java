package com.benhsoan.domain.clinical.exception;

import org.springframework.http.HttpStatus;

public class ClinicalOrderInvalidVisitException extends ClinicalOrderException {

    public ClinicalOrderInvalidVisitException() {
        super(HttpStatus.CONFLICT, "Clinical orders can only be created for an active visit.");
    }
}

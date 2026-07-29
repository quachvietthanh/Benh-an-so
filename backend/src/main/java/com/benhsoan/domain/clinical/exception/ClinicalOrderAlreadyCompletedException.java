package com.benhsoan.domain.clinical.exception;

import org.springframework.http.HttpStatus;

public class ClinicalOrderAlreadyCompletedException extends ClinicalOrderException {

    public ClinicalOrderAlreadyCompletedException() {
        super(HttpStatus.CONFLICT, "Clinical order has already been completed.");
    }
}

package com.benhsoan.domain.clinical.exception;

import org.springframework.http.HttpStatus;

public class ClinicalOrderAlreadyCancelledException extends ClinicalOrderException {

    public ClinicalOrderAlreadyCancelledException() {
        super(HttpStatus.CONFLICT, "Clinical order has already been cancelled.");
    }
}

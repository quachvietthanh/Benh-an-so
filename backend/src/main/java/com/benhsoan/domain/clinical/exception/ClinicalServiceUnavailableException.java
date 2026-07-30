package com.benhsoan.domain.clinical.exception;

import org.springframework.http.HttpStatus;

public class ClinicalServiceUnavailableException extends ClinicalOrderException {

    public ClinicalServiceUnavailableException() {
        super(HttpStatus.CONFLICT, "One or more clinical services are unavailable.");
    }
}

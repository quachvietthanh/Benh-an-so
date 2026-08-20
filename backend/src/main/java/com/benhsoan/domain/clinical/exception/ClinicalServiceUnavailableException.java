package com.benhsoan.domain.clinical.exception;


public class ClinicalServiceUnavailableException extends ClinicalOrderException {

    public ClinicalServiceUnavailableException() {
        super("One or more clinical services are unavailable.");
    }
}

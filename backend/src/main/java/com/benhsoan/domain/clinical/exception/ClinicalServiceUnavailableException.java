package com.benhsoan.domain.clinical.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class ClinicalServiceUnavailableException extends ClinicalOrderException {

    public ClinicalServiceUnavailableException() {
        super(DomainErrorCode.CLINICAL_SERVICE_UNAVAILABLE, "One or more clinical services are unavailable.");
    }
}

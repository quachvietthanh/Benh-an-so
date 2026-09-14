package com.benhsoan.domain.clinical.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class ClinicalServiceCodeAlreadyExistsException extends ClinicalException {

    public ClinicalServiceCodeAlreadyExistsException(String serviceCode) {
        super(DomainErrorCode.CLINICAL_SERVICE_CODE_ALREADY_EXISTS,
                "Clinical service code already exists: " + serviceCode);
    }
}

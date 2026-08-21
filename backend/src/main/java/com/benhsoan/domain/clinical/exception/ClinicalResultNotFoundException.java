package com.benhsoan.domain.clinical.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import java.util.UUID;

public class ClinicalResultNotFoundException extends ClinicalException {

    public ClinicalResultNotFoundException(UUID clinicalResultId) {
        super(DomainErrorCode.CLINICAL_RESULT_NOT_FOUND, "Clinical result not found: " + clinicalResultId);
    }
}

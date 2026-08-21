package com.benhsoan.domain.clinical.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class ClinicalResultAlreadyFinalizedException extends ClinicalResultException {

    public ClinicalResultAlreadyFinalizedException() {
        super(DomainErrorCode.CLINICAL_RESULT_ALREADY_FINALIZED, "Clinical result is already finalized.");
    }
}

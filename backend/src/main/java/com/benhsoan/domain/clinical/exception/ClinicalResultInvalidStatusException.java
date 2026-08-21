package com.benhsoan.domain.clinical.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class ClinicalResultInvalidStatusException extends ClinicalResultException {

    public ClinicalResultInvalidStatusException(String message) {
        super(DomainErrorCode.CLINICAL_RESULT_INVALID_STATUS, message);
    }
}

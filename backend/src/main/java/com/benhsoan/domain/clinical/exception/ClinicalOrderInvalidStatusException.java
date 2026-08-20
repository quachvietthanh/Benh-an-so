package com.benhsoan.domain.clinical.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class ClinicalOrderInvalidStatusException extends ClinicalOrderException {

    public ClinicalOrderInvalidStatusException(String m) {
        super(DomainErrorCode.CLINICAL_ORDER_INVALID_STATUS, m);
    }
}

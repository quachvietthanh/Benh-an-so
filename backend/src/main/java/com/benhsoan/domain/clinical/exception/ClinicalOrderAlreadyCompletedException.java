package com.benhsoan.domain.clinical.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class ClinicalOrderAlreadyCompletedException extends ClinicalOrderException {

    public ClinicalOrderAlreadyCompletedException() {
        super(DomainErrorCode.CLINICAL_ORDER_ALREADY_COMPLETED, "Clinical order has already been completed.");
    }
}

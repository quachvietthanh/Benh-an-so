package com.benhsoan.domain.clinical.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class ClinicalOrderAlreadyCancelledException extends ClinicalOrderException {

    public ClinicalOrderAlreadyCancelledException() {
        super(DomainErrorCode.CLINICAL_ORDER_ALREADY_CANCELLED, "Clinical order has already been cancelled.");
    }
}

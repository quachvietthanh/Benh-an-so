package com.benhsoan.domain.clinical.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class ClinicalOrderInvalidVisitException extends ClinicalOrderException {

    public ClinicalOrderInvalidVisitException() {
        super(DomainErrorCode.CLINICAL_ORDER_INVALID_VISIT, "Clinical orders can only be created for an active visit.");
    }
}

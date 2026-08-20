package com.benhsoan.domain.clinical.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class ClinicalOrderItemInvalidStatusException extends ClinicalOrderItemException {

    public ClinicalOrderItemInvalidStatusException(String m) {
        super(DomainErrorCode.CLINICAL_ORDER_ITEM_INVALID_STATUS, m);
    }
}

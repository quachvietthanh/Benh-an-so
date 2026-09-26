package com.benhsoan.domain.clinical.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import java.util.UUID;

public class ClinicalOrderItemNotFoundException extends ClinicalException {

    public ClinicalOrderItemNotFoundException(UUID clinicalOrderItemId) {
        super(DomainErrorCode.CLINICAL_ORDER_ITEM_NOT_FOUND, "Clinical order item not found: " + clinicalOrderItemId);
    }
}

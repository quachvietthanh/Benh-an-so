package com.benhsoan.domain.clinical.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class ClinicalOrderNotFoundException extends ClinicalOrderException {

    public ClinicalOrderNotFoundException(UUID orderId) {
        super(DomainErrorCode.CLINICAL_ORDER_NOT_FOUND, "Clinical order not found: " + orderId);
    }
}

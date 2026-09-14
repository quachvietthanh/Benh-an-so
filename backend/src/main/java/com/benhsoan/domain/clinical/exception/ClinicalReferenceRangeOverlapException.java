package com.benhsoan.domain.clinical.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class ClinicalReferenceRangeOverlapException extends ClinicalException {

    public ClinicalReferenceRangeOverlapException(String message) {
        super(DomainErrorCode.CLINICAL_REFERENCE_RANGE_OVERLAP, message);
    }
}

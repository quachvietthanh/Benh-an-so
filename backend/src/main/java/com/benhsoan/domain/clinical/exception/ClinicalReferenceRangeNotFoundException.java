package com.benhsoan.domain.clinical.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class ClinicalReferenceRangeNotFoundException extends ClinicalException {

    public ClinicalReferenceRangeNotFoundException(UUID referenceRangeId) {
        super(DomainErrorCode.CLINICAL_REFERENCE_RANGE_NOT_FOUND,
                "Clinical reference range not found: " + referenceRangeId);
    }
}

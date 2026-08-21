package com.benhsoan.domain.prescription.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class PrescriptionClinicalContextConflictException
        extends PrescriptionException {

    public PrescriptionClinicalContextConflictException(String message) {
        super(DomainErrorCode.PRESCRIPTION_CLINICAL_CONTEXT_CONFLICT, message);
    }
}

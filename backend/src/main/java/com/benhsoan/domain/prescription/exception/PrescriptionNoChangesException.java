package com.benhsoan.domain.prescription.exception;

import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class PrescriptionNoChangesException
        extends ValidationException {

    public PrescriptionNoChangesException() {
        super(DomainErrorCode.PRESCRIPTION_NO_CHANGES, "The amended prescription contains no business changes.");
    }
}

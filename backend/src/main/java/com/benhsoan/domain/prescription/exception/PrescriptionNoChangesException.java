package com.benhsoan.domain.prescription.exception;

import com.benhsoan.domain.shared.exception.ValidationException;

public class PrescriptionNoChangesException
        extends ValidationException {

    public PrescriptionNoChangesException() {
        super("The amended prescription contains no business changes.");
    }
}

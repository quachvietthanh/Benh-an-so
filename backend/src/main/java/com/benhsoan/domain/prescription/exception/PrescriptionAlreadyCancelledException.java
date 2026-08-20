package com.benhsoan.domain.prescription.exception;


public class PrescriptionAlreadyCancelledException
        extends PrescriptionException {

    public PrescriptionAlreadyCancelledException() {
        super(
                "Prescription has already been cancelled."
        );
    }
}

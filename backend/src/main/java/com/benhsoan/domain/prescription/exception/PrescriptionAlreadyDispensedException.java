package com.benhsoan.domain.prescription.exception;


public class PrescriptionAlreadyDispensedException
        extends PrescriptionException {

    public PrescriptionAlreadyDispensedException() {
        super(
                "Prescription has already been dispensed."
        );
    }
}

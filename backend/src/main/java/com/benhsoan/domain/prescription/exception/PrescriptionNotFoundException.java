package com.benhsoan.domain.prescription.exception;

import java.util.UUID;


public class PrescriptionNotFoundException extends PrescriptionException {

    public PrescriptionNotFoundException(UUID prescriptionId) {
        super(
                "Prescription not found with id: " + prescriptionId
        );
    }
}

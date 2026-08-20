package com.benhsoan.domain.prescription.exception;

import java.util.UUID;


public class PrescriptionItemNotFoundException
        extends PrescriptionException {

    public PrescriptionItemNotFoundException(UUID prescriptionItemId) {
        super(
                "Prescription item not found with id: "
                + prescriptionItemId
        );
    }
}

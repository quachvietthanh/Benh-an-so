package com.benhsoan.domain.prescription.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

public class PrescriptionItemNotFoundException
        extends PrescriptionException {

    public PrescriptionItemNotFoundException(UUID prescriptionItemId) {
        super(
                HttpStatus.NOT_FOUND,
                "Prescription item not found with id: "
                + prescriptionItemId
        );
    }
}

package com.benhsoan.domain.prescription.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

public class PrescriptionNotFoundException extends PrescriptionException {

    public PrescriptionNotFoundException(UUID prescriptionId) {
        super(
                HttpStatus.NOT_FOUND,
                "Prescription not found with id: " + prescriptionId
        );
    }
}

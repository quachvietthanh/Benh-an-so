package com.benhsoan.domain.prescription.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

public class DuplicateMedicineInPrescriptionException
        extends PrescriptionException {

    public DuplicateMedicineInPrescriptionException(UUID medicineId) {
        super(
                HttpStatus.CONFLICT,
                "Medicine already exists in the prescription: "
                + medicineId
        );
    }
}

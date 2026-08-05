package com.benhsoan.domain.medicine.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

public class MedicineNotFoundException extends MedicineException {

    public MedicineNotFoundException(UUID medicineId) {
        super(
                HttpStatus.NOT_FOUND,
                "Medicine not found with id: " + medicineId
        );
    }
}

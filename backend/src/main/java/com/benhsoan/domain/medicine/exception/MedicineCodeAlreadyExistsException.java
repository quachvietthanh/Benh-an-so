package com.benhsoan.domain.medicine.exception;

import org.springframework.http.HttpStatus;

public class MedicineCodeAlreadyExistsException extends MedicineException {

    public MedicineCodeAlreadyExistsException(String medicineCode) {
        super(
                HttpStatus.CONFLICT,
                "Medicine code already exists: " + medicineCode
        );
    }
}

package com.benhsoan.domain.medicine.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

public class MedicineInactiveException extends MedicineException {

    public MedicineInactiveException(UUID medicineId) {
        super(
                HttpStatus.CONFLICT,
                "Inactive medicine cannot be added to a prescription: "
                + medicineId
        );
    }
}

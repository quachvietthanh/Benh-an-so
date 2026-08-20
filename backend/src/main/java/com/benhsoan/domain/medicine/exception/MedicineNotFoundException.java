package com.benhsoan.domain.medicine.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import java.util.UUID;


public class MedicineNotFoundException extends MedicineException {

    public MedicineNotFoundException(UUID medicineId) {
        super(DomainErrorCode.MEDICINE_NOT_FOUND,
                "Medicine not found with id: " + medicineId
        );
    }
}

package com.benhsoan.domain.medicine.exception;

import java.util.UUID;


public class MedicineNotFoundException extends MedicineException {

    public MedicineNotFoundException(UUID medicineId) {
        super(
                "Medicine not found with id: " + medicineId
        );
    }
}

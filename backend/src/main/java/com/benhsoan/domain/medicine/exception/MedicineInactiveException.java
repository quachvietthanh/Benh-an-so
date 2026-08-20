package com.benhsoan.domain.medicine.exception;

import java.util.UUID;


public class MedicineInactiveException extends MedicineException {

    public MedicineInactiveException(UUID medicineId) {
        super(
                "Inactive medicine cannot be added to a prescription: "
                + medicineId
        );
    }
}

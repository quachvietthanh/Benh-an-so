package com.benhsoan.domain.medicine.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import java.util.UUID;


public class MedicineInactiveException extends MedicineException {

    public MedicineInactiveException(UUID medicineId) {
        super(DomainErrorCode.MEDICINE_INACTIVE,
                "Inactive medicine cannot be added to a prescription: "
                + medicineId
        );
    }
}

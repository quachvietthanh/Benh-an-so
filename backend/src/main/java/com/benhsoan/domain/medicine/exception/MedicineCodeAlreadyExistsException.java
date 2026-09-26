package com.benhsoan.domain.medicine.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class MedicineCodeAlreadyExistsException extends MedicineException {

    public MedicineCodeAlreadyExistsException(String medicineCode) {
        super(DomainErrorCode.MEDICINE_CODE_ALREADY_EXISTS,
                "Medicine code already exists: " + medicineCode
        );
    }
}

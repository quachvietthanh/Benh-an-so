package com.benhsoan.domain.medicine.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


import com.benhsoan.domain.shared.exception.DomainException;

public abstract class MedicineException extends DomainException {

    protected MedicineException(
            DomainErrorCode code,
            String message
    ) {
        super(code, message);
    }
}

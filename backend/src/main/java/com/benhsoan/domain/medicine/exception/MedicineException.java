package com.benhsoan.domain.medicine.exception;

import org.springframework.http.HttpStatus;

import com.benhsoan.domain.shared.exception.DomainException;

public abstract class MedicineException extends DomainException {

    protected MedicineException(
            HttpStatus status,
            String message
    ) {
        super(status, message);
    }
}

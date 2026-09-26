package com.benhsoan.domain.specialty.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;
import com.benhsoan.domain.shared.exception.DomainException;

public class SpecialtyCodeAlreadyExistsException extends DomainException {

    public SpecialtyCodeAlreadyExistsException(String code) {
        super(DomainErrorCode.SPECIALTY_CODE_ALREADY_EXISTS, "Specialty with code already exists: " + code);
    }
}

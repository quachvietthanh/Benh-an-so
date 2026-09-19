package com.benhsoan.domain.specialty.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;
import com.benhsoan.domain.shared.exception.DomainException;

public class SpecialtyNameAlreadyExistsException extends DomainException {

    public SpecialtyNameAlreadyExistsException(String name) {
        super(DomainErrorCode.SPECIALTY_NAME_ALREADY_EXISTS, "Specialty with name already exists: " + name);
    }
}

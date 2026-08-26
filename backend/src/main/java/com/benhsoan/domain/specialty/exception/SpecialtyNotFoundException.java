package com.benhsoan.domain.specialty.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;
import com.benhsoan.domain.shared.exception.DomainException;

public class SpecialtyNotFoundException extends DomainException {

    public SpecialtyNotFoundException(UUID specialtyId) {
        super(DomainErrorCode.SPECIALTY_NOT_FOUND, "Active specialty does not exist: " + specialtyId);
    }
}

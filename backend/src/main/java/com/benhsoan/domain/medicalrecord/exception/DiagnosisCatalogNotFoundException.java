package com.benhsoan.domain.medicalrecord.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class DiagnosisCatalogNotFoundException extends DiagnosisCatalogException {

    public DiagnosisCatalogNotFoundException(UUID diagnosisCatalogId) {
        super(DomainErrorCode.DIAGNOSIS_CATALOG_NOT_FOUND, "Diagnosis catalog not found: " + diagnosisCatalogId);
    }
}

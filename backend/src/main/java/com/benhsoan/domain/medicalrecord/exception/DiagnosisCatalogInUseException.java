package com.benhsoan.domain.medicalrecord.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class DiagnosisCatalogInUseException extends DiagnosisCatalogException {

    public DiagnosisCatalogInUseException(UUID diagnosisCatalogId) {
        super(DomainErrorCode.DIAGNOSIS_CATALOG_IN_USE,
                "Diagnosis catalog entry is in use: " + diagnosisCatalogId + ". Deactivate the entry instead.");
    }
}

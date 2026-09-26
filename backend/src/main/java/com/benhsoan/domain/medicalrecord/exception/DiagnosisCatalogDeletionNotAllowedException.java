package com.benhsoan.domain.medicalrecord.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class DiagnosisCatalogDeletionNotAllowedException extends DiagnosisCatalogException {

    public DiagnosisCatalogDeletionNotAllowedException() {
        super(DomainErrorCode.DIAGNOSIS_CATALOG_DELETE_NOT_ALLOWED,
                "Diagnosis catalog entries cannot be deleted. Deactivate the entry instead.");
    }
}

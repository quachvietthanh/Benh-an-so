package com.benhsoan.domain.medicalrecord.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class DiagnosisCatalogCodeAlreadyExistsException extends DiagnosisCatalogException {

    public DiagnosisCatalogCodeAlreadyExistsException(String code) {
        super(DomainErrorCode.DIAGNOSIS_CATALOG_CODE_ALREADY_EXISTS, "Diagnosis code already exists: " + code);
    }
}

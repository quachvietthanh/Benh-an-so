package com.benhsoan.domain.clinical.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class ClinicalServiceCatalogNotFoundException extends ClinicalException {

    public ClinicalServiceCatalogNotFoundException(UUID clinicalServiceId) {
        super(DomainErrorCode.CLINICAL_SERVICE_CATALOG_NOT_FOUND,
                "Clinical service catalog not found: " + clinicalServiceId);
    }
}

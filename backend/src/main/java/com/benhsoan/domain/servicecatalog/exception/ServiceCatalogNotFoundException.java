package com.benhsoan.domain.servicecatalog.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainException;

public class ServiceCatalogNotFoundException extends DomainException {

    public ServiceCatalogNotFoundException(UUID serviceCatalogId) {
        super(DomainErrorCode.SERVICE_CATALOG_NOT_FOUND, "Service catalog not found: " + serviceCatalogId);
    }
}

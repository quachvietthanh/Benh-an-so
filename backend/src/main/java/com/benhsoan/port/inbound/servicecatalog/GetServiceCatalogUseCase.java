package com.benhsoan.port.inbound.servicecatalog;

import java.util.UUID;

import com.benhsoan.port.dto.result.servicecatalog.ServiceCatalogResult;

public interface GetServiceCatalogUseCase {

    ServiceCatalogResult getById(UUID serviceCatalogId);
}

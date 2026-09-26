package com.benhsoan.port.inbound.servicecatalog;

import com.benhsoan.port.dto.command.servicecatalog.CreateServiceCatalogCommand;
import com.benhsoan.port.dto.result.servicecatalog.ServiceCatalogResult;

public interface CreateServiceCatalogUseCase {

    ServiceCatalogResult create(CreateServiceCatalogCommand command);
}

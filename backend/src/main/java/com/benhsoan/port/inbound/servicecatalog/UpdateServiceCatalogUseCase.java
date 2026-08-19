package com.benhsoan.port.inbound.servicecatalog;

import com.benhsoan.port.dto.command.servicecatalog.UpdateServiceCatalogCommand;
import com.benhsoan.port.dto.result.servicecatalog.ServiceCatalogResult;

public interface UpdateServiceCatalogUseCase {

    ServiceCatalogResult update(UpdateServiceCatalogCommand command);
}

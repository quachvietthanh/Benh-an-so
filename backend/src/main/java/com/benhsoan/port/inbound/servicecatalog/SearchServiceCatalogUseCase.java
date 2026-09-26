package com.benhsoan.port.inbound.servicecatalog;

import org.springframework.data.domain.Page;

import com.benhsoan.port.dto.command.servicecatalog.SearchServiceCatalogQuery;
import com.benhsoan.port.dto.result.servicecatalog.ServiceCatalogResult;

public interface SearchServiceCatalogUseCase {

    Page<ServiceCatalogResult> search(SearchServiceCatalogQuery query);
}

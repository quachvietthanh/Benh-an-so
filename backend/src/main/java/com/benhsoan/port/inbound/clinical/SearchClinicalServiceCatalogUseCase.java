package com.benhsoan.port.inbound.clinical;

import org.springframework.data.domain.Page;

import com.benhsoan.port.dto.command.clinical.SearchClinicalServiceCatalogQuery;
import com.benhsoan.port.dto.result.ClinicalServiceCatalogResult;

public interface SearchClinicalServiceCatalogUseCase {

    Page<ClinicalServiceCatalogResult> search(SearchClinicalServiceCatalogQuery query);
}

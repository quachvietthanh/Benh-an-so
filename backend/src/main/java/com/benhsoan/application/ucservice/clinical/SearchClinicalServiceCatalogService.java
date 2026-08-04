package com.benhsoan.application.ucservice.clinical;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.port.dto.command.clinical.SearchClinicalServiceCatalogQuery;
import com.benhsoan.port.dto.result.ClinicalServiceCatalogResult;
import com.benhsoan.port.inbound.clinical.SearchClinicalServiceCatalogUseCase;
import com.benhsoan.port.outbound.repository.clinical.ClinicalServiceCatalogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchClinicalServiceCatalogService implements SearchClinicalServiceCatalogUseCase {

    private final ClinicalServiceCatalogRepository clinicalServiceCatalogRepository;
    private final ClinicalOrderAuthorizationService authorizationService;
    private final ClinicalOrderResultMapper resultMapper;

    @Override
    public Page<ClinicalServiceCatalogResult> search(SearchClinicalServiceCatalogQuery query) {
        authorizationService.requireReadAccess();
        return clinicalServiceCatalogRepository.findActiveByKeyword(query.keyword(), PageRequest.of(query.page(), query.size()))
                .map(resultMapper::toResult);
    }
}

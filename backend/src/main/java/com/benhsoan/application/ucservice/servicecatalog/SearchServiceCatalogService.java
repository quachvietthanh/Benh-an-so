package com.benhsoan.application.ucservice.servicecatalog;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.servicecatalog.ServicePrice;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.servicecatalog.SearchServiceCatalogQuery;
import com.benhsoan.port.dto.result.servicecatalog.ServiceCatalogResult;
import com.benhsoan.port.inbound.servicecatalog.SearchServiceCatalogUseCase;
import com.benhsoan.port.outbound.repository.servicecatalog.ServiceCatalogRepository;
import com.benhsoan.port.outbound.repository.servicecatalog.ServicePriceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchServiceCatalogService implements SearchServiceCatalogUseCase {

    private final ServiceCatalogRepository serviceCatalogRepository;
    private final ServicePriceRepository servicePriceRepository;
    private final ServiceCatalogResultMapper resultMapper;

    @Override
    public Page<ServiceCatalogResult> search(SearchServiceCatalogQuery query) {
        if (query == null || query.pageable() == null) {
            throw new ValidationException("Service catalog search query and pageable are required.");
        }
        return serviceCatalogRepository.search(query.keyword(), query.active(), query.pageable())
                .map(serviceCatalog -> {
                    List<ServicePrice> history = servicePriceRepository
                            .findAllByServiceCatalogId(serviceCatalog.getId());
                    return resultMapper.toResult(
                            serviceCatalog,
                            history.isEmpty() ? null : history.getFirst()
                    );
                });
    }
}

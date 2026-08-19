package com.benhsoan.application.ucservice.servicecatalog;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.servicecatalog.ServiceCatalog;
import com.benhsoan.domain.servicecatalog.ServicePrice;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.servicecatalog.ServiceCatalogResult;
import com.benhsoan.port.inbound.servicecatalog.GetServiceCatalogUseCase;
import com.benhsoan.port.outbound.repository.servicecatalog.ServiceCatalogRepository;
import com.benhsoan.port.outbound.repository.servicecatalog.ServicePriceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetServiceCatalogService implements GetServiceCatalogUseCase {

    private final ServiceCatalogRepository serviceCatalogRepository;
    private final ServicePriceRepository servicePriceRepository;
    private final ServiceCatalogResultMapper resultMapper;

    @Override
    public ServiceCatalogResult getById(UUID serviceCatalogId) {
        if (serviceCatalogId == null) {
            throw new ValidationException("Service catalog id is required.");
        }
        ServiceCatalog serviceCatalog = serviceCatalogRepository.findById(serviceCatalogId)
                .orElseThrow(() -> new ValidationException(
                        "Service catalog not found: " + serviceCatalogId
                ));
        List<ServicePrice> history = servicePriceRepository.findAllByServiceCatalogId(serviceCatalogId);
        return resultMapper.toResult(serviceCatalog, history.isEmpty() ? null : history.getFirst());
    }
}

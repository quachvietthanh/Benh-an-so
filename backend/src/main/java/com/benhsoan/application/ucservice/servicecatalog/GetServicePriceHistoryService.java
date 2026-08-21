package com.benhsoan.application.ucservice.servicecatalog;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.servicecatalog.exception.ServiceCatalogNotFoundException;
import com.benhsoan.port.dto.result.servicecatalog.ServicePriceResult;
import com.benhsoan.port.inbound.servicecatalog.GetServicePriceHistoryUseCase;
import com.benhsoan.port.outbound.repository.servicecatalog.ServiceCatalogRepository;
import com.benhsoan.port.outbound.repository.servicecatalog.ServicePriceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetServicePriceHistoryService implements GetServicePriceHistoryUseCase {

    private final ServiceCatalogRepository serviceCatalogRepository;
    private final ServicePriceRepository servicePriceRepository;
    private final ServiceCatalogResultMapper resultMapper;

    @Override
    public List<ServicePriceResult> getHistory(UUID serviceCatalogId) {
        if (serviceCatalogId == null) {
            throw new ValidationException("Service catalog id is required.");
        }
        serviceCatalogRepository.findById(serviceCatalogId)
                .orElseThrow(() -> new ServiceCatalogNotFoundException(serviceCatalogId));
        return servicePriceRepository.findAllByServiceCatalogId(serviceCatalogId).stream()
                .map(resultMapper::toResult)
                .toList();
    }
}

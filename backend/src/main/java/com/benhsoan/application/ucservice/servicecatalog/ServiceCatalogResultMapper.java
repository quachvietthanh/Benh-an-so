package com.benhsoan.application.ucservice.servicecatalog;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.servicecatalog.ServiceCatalog;
import com.benhsoan.domain.servicecatalog.ServicePrice;
import com.benhsoan.port.dto.result.servicecatalog.ServiceCatalogResult;
import com.benhsoan.port.dto.result.servicecatalog.ServicePriceResult;

@Component
class ServiceCatalogResultMapper {

    ServiceCatalogResult toResult(ServiceCatalog serviceCatalog, ServicePrice latestPrice) {
        return new ServiceCatalogResult(
                serviceCatalog.getId(),
                serviceCatalog.getServiceCode(),
                serviceCatalog.getServiceName(),
                serviceCatalog.isActive(),
                latestPrice == null ? null : latestPrice.getPrice(),
                latestPrice == null ? null : latestPrice.getEffectiveFrom(),
                serviceCatalog.getCreatedAt(),
                serviceCatalog.getUpdatedAt()
        );
    }

    ServicePriceResult toResult(ServicePrice servicePrice) {
        return new ServicePriceResult(
                servicePrice.getId(),
                servicePrice.getServiceCatalogId(),
                servicePrice.getPrice(),
                servicePrice.getEffectiveFrom(),
                servicePrice.getCreatedAt(),
                servicePrice.getCreatedBy()
        );
    }
}

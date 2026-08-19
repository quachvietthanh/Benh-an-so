package com.benhsoan.persistence.mapper.servicecatalog;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.servicecatalog.ServicePrice;
import com.benhsoan.persistence.entity.servicecatalog.ServicePriceEntity;

@Component
public class ServicePricePersistenceMapper {

    public ServicePrice toDomain(ServicePriceEntity entity) {
        if (entity == null) {
            return null;
        }

        return ServicePrice.restore(
                entity.getId(),
                entity.getServiceCatalogId(),
                entity.getPrice(),
                entity.getEffectiveFrom(),
                entity.getCreatedAt(),
                entity.getCreatedBy()
        );
    }

    public ServicePriceEntity toEntity(ServicePrice domain) {
        if (domain == null) {
            return null;
        }

        return ServicePriceEntity.builder()
                .id(domain.getId())
                .serviceCatalogId(domain.getServiceCatalogId())
                .price(domain.getPrice())
                .effectiveFrom(domain.getEffectiveFrom())
                .createdAt(domain.getCreatedAt())
                .createdBy(domain.getCreatedBy())
                .build();
    }
}

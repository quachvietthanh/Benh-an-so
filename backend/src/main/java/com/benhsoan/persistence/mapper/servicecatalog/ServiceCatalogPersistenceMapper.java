package com.benhsoan.persistence.mapper.servicecatalog;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.servicecatalog.ServiceCatalog;
import com.benhsoan.persistence.entity.servicecatalog.ServiceCatalogEntity;

@Component
public class ServiceCatalogPersistenceMapper {

    public ServiceCatalog toDomain(ServiceCatalogEntity entity) {
        if (entity == null) {
            return null;
        }

        return ServiceCatalog.restore(
                entity.getId(),
                entity.getServiceCode(),
                entity.getServiceName(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public ServiceCatalogEntity toEntity(ServiceCatalog domain) {
        if (domain == null) {
            return null;
        }

        return ServiceCatalogEntity.builder()
                .id(domain.getId())
                .serviceCode(domain.getServiceCode())
                .serviceName(domain.getServiceName())
                .active(domain.isActive())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}

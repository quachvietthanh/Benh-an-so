package com.benhsoan.persistence.mapper.clinical;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.clinical.ClinicalServiceCatalog;
import com.benhsoan.persistence.entity.clinical.ClinicalServiceCatalogEntity;

@Component
public class ClinicalServiceCatalogPersistenceMapper {

    public ClinicalServiceCatalog toDomain(ClinicalServiceCatalogEntity entity) {
        if (entity == null) {
            return null;
        }

        return ClinicalServiceCatalog.restore(
                entity.getId(),
                entity.getServiceCode(),
                entity.getServiceName(),
                entity.getServiceType(),
                entity.getResultDataType(),
                entity.getUnit(),
                entity.getReferenceRange(),
                entity.getDescription(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public ClinicalServiceCatalogEntity toEntity(ClinicalServiceCatalog domain) {
        if (domain == null) {
            return null;
        }

        return ClinicalServiceCatalogEntity.builder()
                .id(domain.getId())
                .serviceCode(domain.getServiceCode())
                .serviceName(domain.getServiceName())
                .serviceType(domain.getServiceType())
                .resultDataType(domain.getResultDataType())
                .unit(domain.getUnit())
                .referenceRange(domain.getReferenceRange())
                .description(domain.getDescription())
                .active(domain.isActive())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}

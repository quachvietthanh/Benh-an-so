package com.benhsoan.persistence.mapper.prescription;

import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.prescription.PrescriptionTemplate;
import com.benhsoan.domain.prescription.PrescriptionTemplateItem;
import com.benhsoan.persistence.entity.prescription.PrescriptionTemplateEntity;
import com.benhsoan.persistence.entity.prescription.PrescriptionTemplateItemEntity;

@Component
public class PrescriptionTemplatePersistenceMapper {

    public PrescriptionTemplate toDomain(
            PrescriptionTemplateEntity entity,
            List<PrescriptionTemplateItemEntity> itemEntities
    ) {
        if (entity == null) {
            return null;
        }
        List<PrescriptionTemplateItem> items = itemEntities == null
                ? List.of()
                : itemEntities.stream()
                        .map(this::toDomainItem)
                        .toList();
        return PrescriptionTemplate.restore(
                entity.getId(),
                entity.getDiagnosisCatalogId(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                items
        );
    }

    public PrescriptionTemplateEntity toEntity(PrescriptionTemplate domain) {
        if (domain == null) {
            return null;
        }
        return PrescriptionTemplateEntity.builder()
                .id(domain.getId())
                .diagnosisCatalogId(domain.getDiagnosisCatalogId())
                .createdBy(domain.getCreatedBy())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public PrescriptionTemplateItemEntity toEntityItem(PrescriptionTemplateItem domain) {
        if (domain == null) {
            return null;
        }
        return PrescriptionTemplateItemEntity.builder()
                .id(domain.getId())
                .templateId(domain.getTemplateId())
                .medicineId(domain.getMedicineId())
                .dosage(domain.getDosage())
                .frequency(domain.getFrequency())
                .route(domain.getRoute())
                .durationDays(domain.getDurationDays())
                .quantity(domain.getQuantity())
                .instructions(domain.getInstructions())
                .sortOrder(domain.getSortOrder())
                .build();
    }

    public PrescriptionTemplateItem toDomainItem(PrescriptionTemplateItemEntity entity) {
        if (entity == null) {
            return null;
        }
        return PrescriptionTemplateItem.restore(
                entity.getId(),
                entity.getTemplateId(),
                entity.getMedicineId(),
                entity.getDosage(),
                entity.getFrequency(),
                entity.getRoute(),
                entity.getDurationDays(),
                entity.getQuantity(),
                entity.getInstructions(),
                entity.getSortOrder()
        );
    }
}

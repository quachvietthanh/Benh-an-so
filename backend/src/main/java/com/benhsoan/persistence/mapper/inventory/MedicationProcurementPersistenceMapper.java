package com.benhsoan.persistence.mapper.inventory;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.inventory.procurement.MedicationProcurementItem;
import com.benhsoan.domain.inventory.procurement.MedicationProcurementPlan;
import com.benhsoan.persistence.entity.inventory.MedicationProcurementItemEntity;
import com.benhsoan.persistence.entity.inventory.MedicationProcurementPlanEntity;

@Component
public class MedicationProcurementPersistenceMapper {

    public MedicationProcurementPlan toDomain(
            MedicationProcurementPlanEntity planEntity,
            List<MedicationProcurementItemEntity> itemEntities
    ) {
        if (planEntity == null) {
            return null;
        }

        List<MedicationProcurementItem> items = new ArrayList<>();
        if (itemEntities != null) {
            for (MedicationProcurementItemEntity itemEntity : itemEntities) {
                items.add(toItemDomain(itemEntity));
            }
        }

        return MedicationProcurementPlan.builder()
                .id(planEntity.getId())
                .planCode(planEntity.getPlanCode())
                .status(planEntity.getStatus())
                .createdBy(planEntity.getCreatedBy())
                .periodStartDate(planEntity.getPeriodStartDate())
                .periodEndDate(planEntity.getPeriodEndDate())
                .note(planEntity.getNote())
                .submittedAt(planEntity.getSubmittedAt())
                .approvedBy(planEntity.getApprovedBy())
                .approvedAt(planEntity.getApprovedAt())
                .rejectionReason(planEntity.getRejectionReason())
                .items(items)
                .createdAt(planEntity.getCreatedAt())
                .updatedAt(planEntity.getUpdatedAt())
                .build();
    }

    public MedicationProcurementPlanEntity toEntity(MedicationProcurementPlan domain) {
        if (domain == null) {
            return null;
        }

        return MedicationProcurementPlanEntity.builder()
                .id(domain.getId())
                .planCode(domain.getPlanCode())
                .status(domain.getStatus())
                .createdBy(domain.getCreatedBy())
                .periodStartDate(domain.getPeriodStartDate())
                .periodEndDate(domain.getPeriodEndDate())
                .totalItems(domain.getTotalItems())
                .totalSuggestedQuantity(domain.getTotalSuggestedQuantity())
                .totalProposedQuantity(domain.getTotalProposedQuantity())
                .totalApprovedQuantity(domain.getTotalApprovedQuantity())
                .note(domain.getNote())
                .submittedAt(domain.getSubmittedAt())
                .approvedBy(domain.getApprovedBy())
                .approvedAt(domain.getApprovedAt())
                .rejectionReason(domain.getRejectionReason())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public MedicationProcurementItem toItemDomain(MedicationProcurementItemEntity entity) {
        if (entity == null) {
            return null;
        }

        return MedicationProcurementItem.builder()
                .id(entity.getId())
                .planId(entity.getPlanId())
                .medicineId(entity.getMedicineId())
                .currentStock(entity.getCurrentStock())
                .minStockThreshold(entity.getMinStockThreshold())
                .previousPeriodConsumption(entity.getPreviousPeriodConsumption())
                .suggestedQuantity(entity.getSuggestedQuantity())
                .proposedQuantity(entity.getProposedQuantity())
                .approvedQuantity(entity.getApprovedQuantity())
                .note(entity.getNote())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public MedicationProcurementItemEntity toItemEntity(MedicationProcurementItem domain) {
        if (domain == null) {
            return null;
        }

        return MedicationProcurementItemEntity.builder()
                .id(domain.getId())
                .planId(domain.getPlanId())
                .medicineId(domain.getMedicineId())
                .currentStock(domain.getCurrentStock())
                .minStockThreshold(domain.getMinStockThreshold())
                .previousPeriodConsumption(domain.getPreviousPeriodConsumption())
                .suggestedQuantity(domain.getSuggestedQuantity())
                .proposedQuantity(domain.getProposedQuantity())
                .approvedQuantity(domain.getApprovedQuantity())
                .note(domain.getNote())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}

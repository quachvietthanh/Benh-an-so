package com.benhsoan.application.ucservice.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.inventory.procurement.MedicationProcurementItem;
import com.benhsoan.domain.inventory.procurement.MedicationProcurementPlan;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.port.dto.result.ProcurementPlanItemResult;
import com.benhsoan.port.dto.result.ProcurementPlanResult;
import com.benhsoan.port.dto.result.ProcurementPlanSummaryResult;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MedicationProcurementResultMapper {

    private final MedicineRepository medicineRepository;

    public ProcurementPlanResult toPlanResult(MedicationProcurementPlan plan) {
        if (plan == null) {
            return null;
        }

        List<UUID> medicineIds = plan.getItems().stream()
                .map(MedicationProcurementItem::getMedicineId)
                .distinct()
                .toList();

        Map<UUID, Medicine> medicineMap = medicineRepository.findAllById(medicineIds).stream()
                .collect(Collectors.toMap(Medicine::getId, Function.identity()));

        return toPlanResult(plan, medicineMap);
    }

    public ProcurementPlanResult toPlanResult(MedicationProcurementPlan plan, Map<UUID, Medicine> medicineMap) {
        if (plan == null) {
            return null;
        }

        List<ProcurementPlanItemResult> itemResults = new ArrayList<>();
        for (MedicationProcurementItem item : plan.getItems()) {
            Medicine med = medicineMap != null ? medicineMap.get(item.getMedicineId()) : null;
            itemResults.add(new ProcurementPlanItemResult(
                    item.getId(),
                    item.getPlanId(),
                    item.getMedicineId(),
                    med != null ? med.getMedicineCode() : "",
                    med != null ? med.getMedicineName() : "",
                    med != null ? med.getUnit() : "",
                    item.getCurrentStock(),
                    item.getMinStockThreshold(),
                    item.getPreviousPeriodConsumption(),
                    item.getSuggestedQuantity(),
                    item.getProposedQuantity(),
                    item.getApprovedQuantity(),
                    item.getNote(),
                    item.getCreatedAt()
            ));
        }

        return new ProcurementPlanResult(
                plan.getId(),
                plan.getPlanCode(),
                plan.getStatus(),
                plan.getCreatedBy(),
                plan.getPeriodStartDate(),
                plan.getPeriodEndDate(),
                plan.getTotalItems(),
                plan.getTotalSuggestedQuantity(),
                plan.getTotalProposedQuantity(),
                plan.getTotalApprovedQuantity(),
                plan.getNote(),
                plan.getSubmittedAt(),
                plan.getApprovedBy(),
                plan.getApprovedAt(),
                plan.getRejectionReason(),
                plan.getCreatedAt(),
                plan.getUpdatedAt(),
                itemResults
        );
    }

    public ProcurementPlanSummaryResult toSummaryResult(MedicationProcurementPlan plan) {
        if (plan == null) {
            return null;
        }

        return new ProcurementPlanSummaryResult(
                plan.getId(),
                plan.getPlanCode(),
                plan.getStatus(),
                plan.getCreatedBy(),
                plan.getPeriodStartDate(),
                plan.getPeriodEndDate(),
                plan.getTotalItems(),
                plan.getTotalProposedQuantity(),
                plan.getTotalApprovedQuantity(),
                plan.getSubmittedAt(),
                plan.getApprovedBy(),
                plan.getApprovedAt(),
                plan.getCreatedAt()
        );
    }
}

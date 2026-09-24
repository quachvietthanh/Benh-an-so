package com.benhsoan.domain.inventory.procurement;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class MedicationProcurementItem {

    private final UUID id;
    private final UUID planId;
    private final UUID medicineId;
    private final int currentStock;
    private final int minStockThreshold;
    private final int previousPeriodConsumption;
    private final int suggestedQuantity;
    private int proposedQuantity;
    private int approvedQuantity;
    private String note;
    private final Instant createdAt;

    public static MedicationProcurementItem create(
            UUID id,
            UUID planId,
            UUID medicineId,
            int currentStock,
            int minStockThreshold,
            int previousPeriodConsumption,
            int suggestedQuantity,
            int proposedQuantity,
            String note,
            Instant createdAt
    ) {
        if (id == null) {
            throw new ValidationException("Mã dòng chi tiết dự trù thuốc không được để trống.");
        }
        if (medicineId == null) {
            throw new ValidationException("Mã thuốc trong dòng dự trù không được để trống.");
        }
        if (proposedQuantity <= 0) {
            throw new ValidationException("Số lượng thuốc đề nghị mua phải lớn hơn 0.");
        }
        if (createdAt == null) {
            throw new ValidationException("Thời gian tạo dòng chi tiết không được để trống.");
        }

        return MedicationProcurementItem.builder()
                .id(id)
                .planId(planId)
                .medicineId(medicineId)
                .currentStock(Math.max(0, currentStock))
                .minStockThreshold(Math.max(0, minStockThreshold))
                .previousPeriodConsumption(Math.max(0, previousPeriodConsumption))
                .suggestedQuantity(Math.max(0, suggestedQuantity))
                .proposedQuantity(proposedQuantity)
                .approvedQuantity(0)
                .note(note)
                .createdAt(createdAt)
                .build();
    }

    public void updateProposedQuantity(int newProposedQuantity, String newNote) {
        if (newProposedQuantity <= 0) {
            throw new ValidationException("Số lượng thuốc đề nghị mua phải lớn hơn 0.");
        }
        this.proposedQuantity = newProposedQuantity;
        if (newNote != null) {
            this.note = newNote;
        }
    }

    public void setApprovedQuantity(int approvedQuantity) {
        if (approvedQuantity < 0) {
            throw new ValidationException("Số lượng thuốc được duyệt không được âm.");
        }
        this.approvedQuantity = approvedQuantity;
    }
}

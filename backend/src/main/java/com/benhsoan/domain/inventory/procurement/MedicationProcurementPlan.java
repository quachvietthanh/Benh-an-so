package com.benhsoan.domain.inventory.procurement;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.benhsoan.domain.inventory.procurement.enums.ProcurementPlanStatus;
import com.benhsoan.domain.inventory.procurement.exception.ProcurementPlanDuplicateMedicineException;
import com.benhsoan.domain.inventory.procurement.exception.ProcurementPlanEmptyItemsException;
import com.benhsoan.domain.inventory.procurement.exception.ProcurementPlanInvalidStatusException;
import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class MedicationProcurementPlan {

    private final UUID id;
    private final String planCode;
    private ProcurementPlanStatus status;
    private final UUID createdBy;
    private final LocalDate periodStartDate;
    private final LocalDate periodEndDate;
    private final List<MedicationProcurementItem> items;
    private String note;
    private Instant submittedAt;
    private UUID approvedBy;
    private Instant approvedAt;
    private String rejectionReason;
    private final Instant createdAt;
    private Instant updatedAt;

    public static MedicationProcurementPlan createDraft(
            UUID id,
            String planCode,
            UUID createdBy,
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            String note,
            List<MedicationProcurementItem> items,
            Instant createdAt
    ) {
        validateCommonFields(id, planCode, createdBy, periodStartDate, periodEndDate, items, createdAt);

        return MedicationProcurementPlan.builder()
                .id(id)
                .planCode(planCode)
                .status(ProcurementPlanStatus.DRAFT)
                .createdBy(createdBy)
                .periodStartDate(periodStartDate)
                .periodEndDate(periodEndDate)
                .note(note)
                .items(new ArrayList<>(items))
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }

    public static MedicationProcurementPlan createAndSubmit(
            UUID id,
            String planCode,
            UUID createdBy,
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            String note,
            List<MedicationProcurementItem> items,
            Instant now
    ) {
        validateCommonFields(id, planCode, createdBy, periodStartDate, periodEndDate, items, now);

        return MedicationProcurementPlan.builder()
                .id(id)
                .planCode(planCode)
                .status(ProcurementPlanStatus.PENDING_APPROVAL)
                .createdBy(createdBy)
                .periodStartDate(periodStartDate)
                .periodEndDate(periodEndDate)
                .note(note)
                .items(new ArrayList<>(items))
                .submittedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void submit(Instant now) {
        if (this.status != ProcurementPlanStatus.DRAFT) {
            throw new ProcurementPlanInvalidStatusException(this.status, "Gửi duyệt phiếu dự trù");
        }
        if (this.items.isEmpty()) {
            throw new ProcurementPlanEmptyItemsException();
        }
        this.status = ProcurementPlanStatus.PENDING_APPROVAL;
        this.submittedAt = now;
        this.updatedAt = now;
    }

    public void approve(UUID approverId, Instant now, Map<UUID, Integer> approvedQuantitiesPerMedicine) {
        if (this.status != ProcurementPlanStatus.PENDING_APPROVAL) {
            throw new ProcurementPlanInvalidStatusException(this.status, "Phê duyệt phiếu dự trù");
        }
        if (approverId == null) {
            throw new ValidationException("Mã người phê duyệt không được để trống.");
        }

        this.status = ProcurementPlanStatus.APPROVED;
        this.approvedBy = approverId;
        this.approvedAt = now;
        this.updatedAt = now;

        for (MedicationProcurementItem item : this.items) {
            if (approvedQuantitiesPerMedicine != null && approvedQuantitiesPerMedicine.containsKey(item.getMedicineId())) {
                Integer customQty = approvedQuantitiesPerMedicine.get(item.getMedicineId());
                if (customQty == null || customQty < 0) {
                    throw new ValidationException("Số lượng phê duyệt cho thuốc không được để trống hoặc mang giá trị âm.");
                }
                item.setApprovedQuantity(customQty);
            } else {
                item.setApprovedQuantity(item.getProposedQuantity());
            }
        }
    }

    public void reject(UUID approverId, String reason, Instant now) {
        if (this.status != ProcurementPlanStatus.PENDING_APPROVAL) {
            throw new ProcurementPlanInvalidStatusException(this.status, "Từ chối phiếu dự trù");
        }
        if (approverId == null) {
            throw new ValidationException("Mã người từ chối không được để trống.");
        }
        if (reason == null || reason.trim().length() < 5) {
            throw new ValidationException("Lý do từ chối phiếu dự trù mua thuốc phải có ít nhất 5 ký tự.");
        }

        this.status = ProcurementPlanStatus.REJECTED;
        this.approvedBy = approverId;
        this.approvedAt = now;
        this.rejectionReason = reason.trim();
        this.updatedAt = now;

        for (MedicationProcurementItem item : this.items) {
            item.setApprovedQuantity(0);
        }
    }

    public void cancel(UUID actorId, Instant now) {
        if (this.status != ProcurementPlanStatus.DRAFT && this.status != ProcurementPlanStatus.PENDING_APPROVAL) {
            throw new ProcurementPlanInvalidStatusException(this.status, "Hủy phiếu dự trù");
        }
        this.status = ProcurementPlanStatus.CANCELLED;
        this.updatedAt = now;
    }

    public void update(String newNote, List<MedicationProcurementItem> newItems, Instant now) {
        if (this.status != ProcurementPlanStatus.DRAFT && this.status != ProcurementPlanStatus.PENDING_APPROVAL) {
            throw new ProcurementPlanInvalidStatusException(this.status, "Cập nhật phiếu dự trù");
        }
        if (newItems == null || newItems.isEmpty()) {
            throw new ProcurementPlanEmptyItemsException();
        }
        validateDuplicateMedicines(newItems);

        this.note = newNote;
        this.items.clear();
        this.items.addAll(newItems);
        this.updatedAt = now;
    }

    public int getTotalItems() {
        return this.items.size();
    }

    public int getTotalSuggestedQuantity() {
        return this.items.stream().mapToInt(MedicationProcurementItem::getSuggestedQuantity).sum();
    }

    public int getTotalProposedQuantity() {
        return this.items.stream().mapToInt(MedicationProcurementItem::getProposedQuantity).sum();
    }

    public int getTotalApprovedQuantity() {
        return this.items.stream().mapToInt(MedicationProcurementItem::getApprovedQuantity).sum();
    }

    private static void validateCommonFields(
            UUID id,
            String planCode,
            UUID createdBy,
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            List<MedicationProcurementItem> items,
            Instant createdAt
    ) {
        if (id == null) {
            throw new ValidationException("Mã định danh phiếu dự trù không được để trống.");
        }
        if (planCode == null || planCode.isBlank()) {
            throw new ValidationException("Mã phiếu dự trù mua thuốc không được để trống.");
        }
        if (createdBy == null) {
            throw new ValidationException("Mã người tạo phiếu dự trù không được để trống.");
        }
        if (periodStartDate == null || periodEndDate == null) {
            throw new ValidationException("Kỳ tham chiếu ngày bắt đầu và ngày kết thúc không được để trống.");
        }
        if (periodEndDate.isBefore(periodStartDate)) {
            throw new ValidationException("Ngày kết thúc kỳ tham chiếu không được trước ngày bắt đầu.");
        }
        if (items == null || items.isEmpty()) {
            throw new ProcurementPlanEmptyItemsException();
        }
        if (createdAt == null) {
            throw new ValidationException("Thời gian tạo phiếu không được để trống.");
        }
        validateDuplicateMedicines(items);
    }

    private static void validateDuplicateMedicines(List<MedicationProcurementItem> items) {
        Set<UUID> seenMedicines = new HashSet<>();
        for (MedicationProcurementItem item : items) {
            if (!seenMedicines.add(item.getMedicineId())) {
                throw new ProcurementPlanDuplicateMedicineException(item.getMedicineId());
            }
        }
    }
}

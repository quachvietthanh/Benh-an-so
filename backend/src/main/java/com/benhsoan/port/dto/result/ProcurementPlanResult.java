package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.inventory.procurement.enums.ProcurementPlanStatus;

public record ProcurementPlanResult(
        UUID id,
        String planCode,
        ProcurementPlanStatus status,
        UUID createdBy,
        LocalDate periodStartDate,
        LocalDate periodEndDate,
        int totalItems,
        int totalSuggestedQuantity,
        int totalProposedQuantity,
        int totalApprovedQuantity,
        String note,
        Instant submittedAt,
        UUID approvedBy,
        Instant approvedAt,
        String rejectionReason,
        Instant createdAt,
        Instant updatedAt,
        List<ProcurementPlanItemResult> items
) {
}

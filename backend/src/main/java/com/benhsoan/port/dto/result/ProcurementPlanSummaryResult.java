package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.inventory.procurement.enums.ProcurementPlanStatus;

public record ProcurementPlanSummaryResult(
        UUID id,
        String planCode,
        ProcurementPlanStatus status,
        UUID createdBy,
        LocalDate periodStartDate,
        LocalDate periodEndDate,
        int totalItems,
        int totalProposedQuantity,
        int totalApprovedQuantity,
        Instant submittedAt,
        UUID approvedBy,
        Instant approvedAt,
        Instant createdAt
) {
}

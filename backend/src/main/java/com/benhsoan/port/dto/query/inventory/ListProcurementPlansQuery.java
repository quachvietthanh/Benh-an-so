package com.benhsoan.port.dto.query.inventory;

import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.inventory.procurement.enums.ProcurementPlanStatus;

public record ListProcurementPlansQuery(
        ProcurementPlanStatus status,
        LocalDate fromDate,
        LocalDate toDate,
        UUID createdBy,
        int page,
        int size
) {
}

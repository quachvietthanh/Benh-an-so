package com.benhsoan.port.outbound.repository.inventory;

import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.inventory.procurement.enums.ProcurementPlanStatus;

public record MedicationProcurementPlanSearchCriteria(
        ProcurementPlanStatus status,
        LocalDate fromDate,
        LocalDate toDate,
        UUID createdBy
) {
}

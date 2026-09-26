package com.benhsoan.port.inbound.inventory;

import org.springframework.data.domain.Page;

import com.benhsoan.port.dto.query.inventory.ListProcurementPlansQuery;
import com.benhsoan.port.dto.result.ProcurementPlanSummaryResult;

public interface ListMedicationProcurementPlansUseCase {

    Page<ProcurementPlanSummaryResult> list(ListProcurementPlansQuery query);
}

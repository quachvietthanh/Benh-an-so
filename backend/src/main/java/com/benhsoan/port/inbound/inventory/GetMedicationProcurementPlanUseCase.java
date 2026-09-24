package com.benhsoan.port.inbound.inventory;

import java.util.UUID;

import com.benhsoan.port.dto.result.ProcurementPlanResult;

public interface GetMedicationProcurementPlanUseCase {

    ProcurementPlanResult getById(UUID id);

    ProcurementPlanResult getByPlanCode(String planCode);
}

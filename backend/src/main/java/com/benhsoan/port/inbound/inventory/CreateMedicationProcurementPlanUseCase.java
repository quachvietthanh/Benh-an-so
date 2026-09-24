package com.benhsoan.port.inbound.inventory;

import com.benhsoan.port.dto.command.inventory.CreateProcurementPlanCommand;
import com.benhsoan.port.dto.result.ProcurementPlanResult;

public interface CreateMedicationProcurementPlanUseCase {

    ProcurementPlanResult create(CreateProcurementPlanCommand command);
}

package com.benhsoan.port.inbound.inventory;

import com.benhsoan.port.dto.command.inventory.ApproveProcurementPlanCommand;
import com.benhsoan.port.dto.result.ProcurementPlanResult;

public interface ApproveMedicationProcurementPlanUseCase {

    ProcurementPlanResult approve(ApproveProcurementPlanCommand command);
}

package com.benhsoan.port.inbound.inventory;

import com.benhsoan.port.dto.command.inventory.RejectProcurementPlanCommand;
import com.benhsoan.port.dto.result.ProcurementPlanResult;

public interface RejectMedicationProcurementPlanUseCase {

    ProcurementPlanResult reject(RejectProcurementPlanCommand command);
}

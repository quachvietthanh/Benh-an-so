package com.benhsoan.port.inbound.inventory;

import java.util.UUID;

import com.benhsoan.port.dto.command.inventory.UpdateProcurementPlanCommand;
import com.benhsoan.port.dto.result.ProcurementPlanResult;

public interface UpdateMedicationProcurementPlanUseCase {

    ProcurementPlanResult update(UpdateProcurementPlanCommand command);

    ProcurementPlanResult submit(UUID id);

    void cancel(UUID id);
}

package com.benhsoan.port.dto.command.prescription;

import java.util.List;
import java.util.UUID;

public record ReturnMedicationCommand(
        UUID prescriptionId,
        String reason,
        List<ReturnMedicationItemCommand> items
) {
}

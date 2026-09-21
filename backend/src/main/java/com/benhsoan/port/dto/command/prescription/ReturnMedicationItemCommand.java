package com.benhsoan.port.dto.command.prescription;

import java.util.UUID;

public record ReturnMedicationItemCommand(
        UUID dispenseItemId,
        int quantity
) {
}

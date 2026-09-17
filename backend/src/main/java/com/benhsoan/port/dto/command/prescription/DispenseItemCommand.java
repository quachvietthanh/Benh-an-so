package com.benhsoan.port.dto.command.prescription;

import java.util.UUID;

/**
 * A single medicine to dispense for a prescription. {@code quantity} is the
 * actual quantity the pharmacist wants to dispense in this event.
 */
public record DispenseItemCommand(
        UUID prescriptionItemId,
        int quantity
) {
}
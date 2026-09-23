package com.benhsoan.port.dto.command.prescription;

import java.util.List;
import java.util.UUID;

/**
 * Command for partial/full dispensing of a prescription with explicit,
 * per-item actual quantities. When {@code items} is empty, the service
 * dispenses the remaining quantity of every item (full completion).
 */
public record DispensePrescriptionItemsCommand(
        UUID prescriptionId,
        List<DispenseItemCommand> items,
        boolean controlledMedicineConfirmed
) {
    public DispensePrescriptionItemsCommand {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public DispensePrescriptionItemsCommand(UUID prescriptionId, List<DispenseItemCommand> items) {
        this(prescriptionId, items, false);
    }
}
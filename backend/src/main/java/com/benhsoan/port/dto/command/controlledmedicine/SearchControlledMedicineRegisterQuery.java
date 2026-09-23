package com.benhsoan.port.dto.command.controlledmedicine;

import java.time.Instant;
import java.util.UUID;

public record SearchControlledMedicineRegisterQuery(
        UUID patientId,
        UUID medicineId,
        Instant from,
        Instant to,
        int page,
        int size
) {
}

package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.prescription.enums.PrescriptionStatus;

public record ReturnMedicationResult(
        UUID prescriptionId,
        PrescriptionStatus status,
        UUID returnedBy,
        Instant returnedAt,
        List<ReturnedMedicationItemResult> returns
) {
}

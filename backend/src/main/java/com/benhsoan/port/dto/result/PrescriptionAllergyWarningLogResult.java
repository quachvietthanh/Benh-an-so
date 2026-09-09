package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.patient.enums.AllergySeverity;

public record PrescriptionAllergyWarningLogResult(
        UUID id,
        UUID prescriptionId,
        String prescriptionCode,
        UUID patientId,
        String patientCode,
        String patientName,
        UUID doctorId,
        String doctorName,
        UUID medicineId,
        String medicineName,
        String activeIngredient,
        String allergenName,
        AllergySeverity severity,
        String reaction,
        String overrideReason,
        Instant handledAt
) {
}

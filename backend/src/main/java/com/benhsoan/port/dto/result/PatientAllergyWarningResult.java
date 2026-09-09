package com.benhsoan.port.dto.result;

import java.util.UUID;

import com.benhsoan.domain.patient.enums.AllergySeverity;

public record PatientAllergyWarningResult(
        UUID allergyId,
        UUID patientId,
        UUID medicineId,
        String medicineName,
        String activeIngredient,
        String allergenName,
        AllergySeverity severity,
        String reaction
) {
}

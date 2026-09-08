package com.benhsoan.adapter.inbound.rest.response.prescription;

import java.util.UUID;

import com.benhsoan.domain.patient.enums.AllergySeverity;

public record PatientAllergyWarningResponse(
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

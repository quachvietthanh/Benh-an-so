package com.benhsoan.application.ucservice.prescription.snapshot;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;

public record PrescriptionSnapshot(
        int schemaVersion,
        UUID id,
        String prescriptionCode,
        UUID medicalRecordId,
        PrescriptionStatus status,
        String note,
        UUID prescribedBy,
        Instant prescribedAt,
        UUID updatedBy,
        Instant updatedAt,
        List<Item> items
) {

    public record Item(
            UUID id,
            UUID medicineId,
            String medicineName,
            String activeIngredient,
            String strength,
            String unit,
            String dosage,
            Integer frequency,
            AdministrationRoute route,
            Integer durationDays,
            int quantity,
            String instructions,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}

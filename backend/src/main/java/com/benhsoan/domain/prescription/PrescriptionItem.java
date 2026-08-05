package com.benhsoan.domain.prescription;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PrescriptionItem {

    private UUID id;

    private UUID prescriptionId;

    private UUID medicineId;

    private String medicineName;

    private String activeIngredient;

    private String strength;

    private String unit;

    private String dosage;

    private String frequency;

    private AdministrationRoute route;

    private Integer durationDays;

    private int quantity;

    private String instructions;

    private Instant createdAt;

    private Instant updatedAt;

    private PrescriptionItem(
            UUID id,
            UUID prescriptionId,
            UUID medicineId,
            String medicineName,
            String activeIngredient,
            String strength,
            String unit,
            String dosage,
            String frequency,
            AdministrationRoute route,
            Integer durationDays,
            int quantity,
            String instructions,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = requireNonNull(id, "Prescription item id is required.");
        this.prescriptionId = requireNonNull(prescriptionId, "Prescription id is required.");
        this.medicineId = requireNonNull(medicineId, "Medicine id is required.");
        this.medicineName = requireText(medicineName, "Medicine name snapshot is required.");
        this.activeIngredient = requireText(activeIngredient, "Active ingredient snapshot is required.");
        this.strength = requireText(strength, "Medicine strength snapshot is required.");
        this.unit = requireText(unit, "Medicine unit snapshot is required.");
        this.dosage = requireText(dosage, "Dosage is required.");
        this.frequency = requireText(frequency, "Frequency is required.");
        this.route = requireNonNull(route, "Administration route is required.");
        this.durationDays = validateDurationDays(durationDays);
        this.quantity = validateQuantity(quantity);
        this.instructions = normalizeOptionalText(instructions);
        this.createdAt = requireNonNull(createdAt, "Prescription item creation time is required.");
        this.updatedAt = updatedAt;
    }

    public static PrescriptionItem create(
            UUID id,
            UUID prescriptionId,
            UUID medicineId,
            String medicineName,
            String activeIngredient,
            String strength,
            String unit,
            String dosage,
            String frequency,
            AdministrationRoute route,
            Integer durationDays,
            int quantity,
            String instructions,
            Instant createdAt
    ) {
        return new PrescriptionItem(
                id,
                prescriptionId,
                medicineId,
                medicineName,
                activeIngredient,
                strength,
                unit,
                dosage,
                frequency,
                route,
                durationDays,
                quantity,
                instructions,
                createdAt,
                null
        );
    }

    public static PrescriptionItem restore(
            UUID id,
            UUID prescriptionId,
            UUID medicineId,
            String medicineName,
            String activeIngredient,
            String strength,
            String unit,
            String dosage,
            String frequency,
            AdministrationRoute route,
            Integer durationDays,
            int quantity,
            String instructions,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new PrescriptionItem(
                id,
                prescriptionId,
                medicineId,
                medicineName,
                activeIngredient,
                strength,
                unit,
                dosage,
                frequency,
                route,
                durationDays,
                quantity,
                instructions,
                createdAt,
                updatedAt
        );
    }

    private static int validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new ValidationException("Prescription item quantity must be greater than zero.");
        }
        return quantity;
    }

    private static Integer validateDurationDays(Integer durationDays) {
        if (durationDays != null && durationDays <= 0) {
            throw new ValidationException("Prescription duration must be greater than zero days.");
        }
        return durationDays;
    }

    private static String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(message);
        }
        return value.trim();
    }

    private static <T> T requireNonNull(T value, String message) {
        if (Objects.isNull(value)) {
            throw new ValidationException(message);
        }
        return value;
    }
}

package com.benhsoan.domain.prescription;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import java.math.BigDecimal;

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

    private Integer frequency;

    private AdministrationRoute route;

    private Integer durationDays;

    private int quantity;

    private int dispensedQuantity;

    private String instructions;

    private BigDecimal singleDoseQuantity;

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
            Integer frequency,
            AdministrationRoute route,
            Integer durationDays,
            int quantity,
            int dispensedQuantity,
            String instructions,
            BigDecimal singleDoseQuantity,
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
        this.frequency = validateFrequency(frequency);
        this.route = requireNonNull(route, "Administration route is required.");
        this.durationDays = validateDurationDays(durationDays);
        this.quantity = validateQuantity(quantity);
        this.dispensedQuantity = validateDispensedQuantity(dispensedQuantity, this.quantity);
        this.instructions = normalizeOptionalText(instructions);
        this.singleDoseQuantity = validateOptionalPositive(singleDoseQuantity);
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
            Integer frequency,
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
                0,
                instructions,
                null,
                createdAt,
                null
        );
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
            Integer frequency,
            AdministrationRoute route,
            Integer durationDays,
            int quantity,
            String instructions,
            BigDecimal singleDoseQuantity,
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
                0,
                instructions,
                singleDoseQuantity,
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
            Integer frequency,
            AdministrationRoute route,
            Integer durationDays,
            int quantity,
            String instructions,
            Instant createdAt,
            Instant updatedAt
    ) {
        return restore(
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
                0,
                instructions,
                createdAt,
                updatedAt
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
            Integer frequency,
            AdministrationRoute route,
            Integer durationDays,
            int quantity,
            int dispensedQuantity,
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
                dispensedQuantity,
                instructions,
                null,
                createdAt,
                updatedAt
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
            Integer frequency,
            AdministrationRoute route,
            Integer durationDays,
            int quantity,
            int dispensedQuantity,
            String instructions,
            BigDecimal singleDoseQuantity,
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
                dispensedQuantity,
                instructions,
                singleDoseQuantity,
                createdAt,
                updatedAt
        );
    }

    public void recordDispense(int additionalQuantity) {
        if (additionalQuantity <= 0) {
            throw new ValidationException("Dispensed quantity must be greater than zero.");
        }
        int newDispensedQuantity = this.dispensedQuantity + additionalQuantity;
        if (newDispensedQuantity > this.quantity) {
            throw new ValidationException("Dispensed quantity cannot exceed the prescribed quantity.");
        }
        this.dispensedQuantity = newDispensedQuantity;
    }

    public void recordReturn(int returnedQuantity) {
        if (returnedQuantity <= 0) {
            throw new ValidationException("Returned quantity must be greater than zero.");
        }
        if (returnedQuantity > this.dispensedQuantity) {
            throw new ValidationException("Returned quantity cannot exceed the dispensed quantity.");
        }
        this.dispensedQuantity -= returnedQuantity;
    }

    public int getRemainingQuantity() {
        return this.quantity - this.dispensedQuantity;
    }

    public boolean isFullyDispensed() {
        return this.dispensedQuantity >= this.quantity;
    }

    private static int validateDispensedQuantity(int dispensedQuantity, int quantity) {
        if (dispensedQuantity < 0) {
            throw new ValidationException("Dispensed quantity must not be negative.");
        }
        if (dispensedQuantity > quantity) {
            throw new ValidationException("Dispensed quantity cannot exceed the prescribed quantity.");
        }
        return dispensedQuantity;
    }

    private static int validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new ValidationException("Prescription item quantity must be greater than zero.");
        }
        return quantity;
    }

    private static BigDecimal validateOptionalPositive(BigDecimal value) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Single dose quantity must be greater than zero.");
        }
        return value;
    }

    private static Integer validateDurationDays(Integer durationDays) {
        if (durationDays == null || durationDays <= 0) {
            throw new ValidationException("Prescription duration must be greater than zero days.");
        }
        return durationDays;
    }

    private static Integer validateFrequency(Integer frequency) {
        if (frequency == null || frequency <= 0) {
            throw new ValidationException(
                    "Prescription frequency must be greater than zero per day."
            );
        }
        return frequency;
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

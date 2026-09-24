package com.benhsoan.domain.prescription;

import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * NCL-05-CN-008: a single medicine entry inside a prescription template. Stores the
 * instruction fields required by QTN-20 (dosage, frequency, duration, route) and the
 * quantity, but NOT medicine catalog snapshots (name / active ingredient / strength),
 * which are resolved from the live {@code medicineId} at application time.
 */
@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PrescriptionTemplateItem {

    private UUID id;
    private UUID templateId;
    private UUID medicineId;
    private String dosage;
    private Integer frequency;
    private AdministrationRoute route;
    private Integer durationDays;
    private int quantity;
    private String instructions;
    private int sortOrder;

    private PrescriptionTemplateItem(
            UUID id,
            UUID templateId,
            UUID medicineId,
            String dosage,
            Integer frequency,
            AdministrationRoute route,
            Integer durationDays,
            int quantity,
            String instructions,
            int sortOrder
    ) {
        this.id = requireNonNull(id, "Template item id is required.");
        this.templateId = requireNonNull(templateId, "Template id is required.");
        this.medicineId = requireNonNull(medicineId, "Medicine id is required.");
        this.dosage = requireText(dosage, "Dosage is required.");
        this.frequency = requirePositive(frequency, "Frequency must be greater than zero per day.");
        this.route = requireNonNull(route, "Administration route is required.");
        this.durationDays = requirePositive(durationDays, "Duration must be greater than zero days.");
        this.quantity = requirePositiveQuantity(quantity);
        this.instructions = normalizeOptionalText(instructions);
        this.sortOrder = requireNonNegative(sortOrder);
    }

    public static PrescriptionTemplateItem create(
            UUID templateId,
            UUID medicineId,
            String dosage,
            Integer frequency,
            AdministrationRoute route,
            Integer durationDays,
            int quantity,
            String instructions,
            int sortOrder
    ) {
        return new PrescriptionTemplateItem(
                UUID.randomUUID(),
                templateId,
                medicineId,
                dosage,
                frequency,
                route,
                durationDays,
                quantity,
                instructions,
                sortOrder
        );
    }

    public static PrescriptionTemplateItem restore(
            UUID id,
            UUID templateId,
            UUID medicineId,
            String dosage,
            Integer frequency,
            AdministrationRoute route,
            Integer durationDays,
            int quantity,
            String instructions,
            int sortOrder
    ) {
        return new PrescriptionTemplateItem(
                id,
                templateId,
                medicineId,
                dosage,
                frequency,
                route,
                durationDays,
                quantity,
                instructions,
                sortOrder
        );
    }

    private static int requireNonNegative(int value) {
        if (value < 0) {
            throw new ValidationException("Sort order must not be negative.");
        }
        return value;
    }

    private static int requirePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new ValidationException("Quantity must be greater than zero.");
        }
        return quantity;
    }

    private static Integer requirePositive(Integer value, String message) {
        if (value == null || value <= 0) {
            throw new ValidationException(message);
        }
        return value;
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

package com.benhsoan.domain.medicine;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;
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
public class Medicine {

    private UUID id;

    private String medicineCode;

    private String medicineName;

    private String activeIngredient;

    private String strength;

    private DosageForm dosageForm;

    private String unit;

    private AdministrationRoute defaultRoute;

    private boolean active;

    private Instant createdAt;

    private Instant updatedAt;

    private int stockQuantity;

    private int minStockThreshold;

    private Medicine(
            UUID id,
            String medicineCode,
            String medicineName,
            String activeIngredient,
            String strength,
            DosageForm dosageForm,
            String unit,
            AdministrationRoute defaultRoute,
            boolean active,
            Instant createdAt,
            Instant updatedAt,
            int stockQuantity,
            int minStockThreshold
    ) {
        this.id = requireNonNull(id, "Medicine id is required.");
        this.medicineCode = requireText(medicineCode, "Medicine code is required.");
        this.medicineName = requireText(medicineName, "Medicine name is required.");
        this.activeIngredient = requireText(activeIngredient, "Active ingredient is required.");
        this.strength = requireText(strength, "Medicine strength is required.");
        this.dosageForm = requireNonNull(dosageForm, "Dosage form is required.");
        this.unit = requireText(unit, "Medicine unit is required.");
        this.defaultRoute = requireNonNull(defaultRoute, "Default administration route is required.");
        this.active = active;
        this.createdAt = requireNonNull(createdAt, "Medicine creation time is required.");
        this.updatedAt = updatedAt;
        this.stockQuantity = requireNonNegative(stockQuantity, "Medicine stock quantity must not be negative.");
        this.minStockThreshold = requireNonNegative(
                minStockThreshold,
                "Medicine minimum stock threshold must not be negative."
        );
    }

    public static Medicine create(
            UUID id,
            String medicineCode,
            String medicineName,
            String activeIngredient,
            String strength,
            DosageForm dosageForm,
            String unit,
            AdministrationRoute defaultRoute,
            int minStockThreshold,
            Instant createdAt
    ) {
        return new Medicine(
                id,
                medicineCode,
                medicineName,
                activeIngredient,
                strength,
                dosageForm,
                unit,
                defaultRoute,
                true,
                createdAt,
                null,
                0,
                minStockThreshold
        );
    }

    public static Medicine restore(
            UUID id,
            String medicineCode,
            String medicineName,
            String activeIngredient,
            String strength,
            DosageForm dosageForm,
            String unit,
            AdministrationRoute defaultRoute,
            boolean active,
            Instant createdAt,
            Instant updatedAt,
            int stockQuantity,
            int minStockThreshold
    ) {
        return new Medicine(
                id,
                medicineCode,
                medicineName,
                activeIngredient,
                strength,
                dosageForm,
                unit,
                defaultRoute,
                active,
                createdAt,
                updatedAt,
                stockQuantity,
                minStockThreshold
        );
    }

    public void updateInformation(
            String medicineName,
            String activeIngredient,
            String strength,
            DosageForm dosageForm,
            String unit,
            AdministrationRoute defaultRoute,
            int minStockThreshold,
            Instant updatedAt
    ) {
        String validatedMedicineName = requireText(medicineName, "Medicine name is required.");
        String validatedActiveIngredient = requireText(activeIngredient, "Active ingredient is required.");
        String validatedStrength = requireText(strength, "Medicine strength is required.");
        DosageForm validatedDosageForm = requireNonNull(dosageForm, "Dosage form is required.");
        String validatedUnit = requireText(unit, "Medicine unit is required.");
        AdministrationRoute validatedDefaultRoute = requireNonNull(
                defaultRoute,
                "Default administration route is required."
        );
        int validatedMinStockThreshold = requireNonNegative(
                minStockThreshold,
                "Medicine minimum stock threshold must not be negative."
        );
        Instant validatedUpdatedAt = requireNonNull(updatedAt, "Medicine update time is required.");

        this.medicineName = validatedMedicineName;
        this.activeIngredient = validatedActiveIngredient;
        this.strength = validatedStrength;
        this.dosageForm = validatedDosageForm;
        this.unit = validatedUnit;
        this.defaultRoute = validatedDefaultRoute;
        this.minStockThreshold = validatedMinStockThreshold;
        this.updatedAt = validatedUpdatedAt;
    }

    public void activate(Instant updatedAt) {
        Instant validatedUpdatedAt = requireNonNull(updatedAt, "Medicine update time is required.");
        this.active = true;
        this.updatedAt = validatedUpdatedAt;
    }

    public void deactivate(Instant updatedAt) {
        Instant validatedUpdatedAt = requireNonNull(updatedAt, "Medicine update time is required.");
        this.active = false;
        this.updatedAt = validatedUpdatedAt;
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

    private static int requireNonNegative(int value, String message) {
        if (value < 0) {
            throw new ValidationException(message);
        }
        return value;
    }
}

package com.benhsoan.domain.medicine;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * NCL-05-CN-007: persistent flag recording that a medicine's max-daily-dose
 * catalog configuration is missing or inconsistent.
 *
 * <p>This is NOT a warning and does NOT block prescription creation/amendment
 * (TC-04). It exists so the catalog administrator can identify and correct
 * medicines that still lack {@code strengthValueMg} or {@code maxDailyDoseMg}.
 *
 * <p>One active flag exists per {@code (medicineId, missingReason)}; repeated
 * prescriptions of the same still-missing medicine update {@code lastDetectedAt}
 * instead of creating duplicate rows.
 */
@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicineMaxDailyDoseMissingData {

    public static final String REASON_MAX_DAILY_DOSE = "MAX_DAILY_DOSE";
    public static final String REASON_STRENGTH_VALUE = "STRENGTH_VALUE_MG";

    private UUID id;
    private UUID medicineId;
    private String activeIngredient;
    private String missingReason;
    private Instant firstDetectedAt;
    private Instant lastDetectedAt;

    private MedicineMaxDailyDoseMissingData(
            UUID id,
            UUID medicineId,
            String activeIngredient,
            String missingReason,
            Instant firstDetectedAt,
            Instant lastDetectedAt
    ) {
        this.id = requireNonNull(id, "Missing-data flag id is required.");
        this.medicineId = requireNonNull(medicineId, "Medicine id is required.");
        this.activeIngredient = requireText(activeIngredient, "Active ingredient is required.");
        this.missingReason = requireText(missingReason, "Missing reason is required.");
        this.firstDetectedAt = requireNonNull(firstDetectedAt, "First detected time is required.");
        this.lastDetectedAt = requireNonNull(lastDetectedAt, "Last detected time is required.");
    }

    public static MedicineMaxDailyDoseMissingData create(
            UUID id,
            UUID medicineId,
            String activeIngredient,
            String missingReason,
            Instant detectedAt
    ) {
        return new MedicineMaxDailyDoseMissingData(
                id,
                medicineId,
                activeIngredient,
                missingReason,
                detectedAt,
                detectedAt
        );
    }

    public static MedicineMaxDailyDoseMissingData restore(
            UUID id,
            UUID medicineId,
            String activeIngredient,
            String missingReason,
            Instant firstDetectedAt,
            Instant lastDetectedAt
    ) {
        return new MedicineMaxDailyDoseMissingData(
                id,
                medicineId,
                activeIngredient,
                missingReason,
                firstDetectedAt,
                lastDetectedAt
        );
    }

    public MedicineMaxDailyDoseMissingData markDetectedAgain(Instant detectedAt) {
        Instant validatedDetectedAt = requireNonNull(detectedAt, "Detected time is required.");
        return new MedicineMaxDailyDoseMissingData(
                this.id,
                this.medicineId,
                this.activeIngredient,
                this.missingReason,
                this.firstDetectedAt,
                validatedDetectedAt
        );
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

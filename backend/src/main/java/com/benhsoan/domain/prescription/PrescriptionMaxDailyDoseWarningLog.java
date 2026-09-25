package com.benhsoan.domain.prescription;

import java.math.BigDecimal;
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
 * NCL-05-CN-007: business warning-override log for a max-daily-dose exceedance.
 * This is a domain/business log, NOT a replacement for the central audit mechanism.
 */
@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PrescriptionMaxDailyDoseWarningLog {

    private UUID id;
    private UUID prescriptionId;
    private UUID patientId;
    private String activeIngredient;
    private BigDecimal totalDailyDoseMg;
    private BigDecimal maxDailyDoseMg;
    private String overrideReason;
    private UUID handledBy;
    private Instant handledAt;
    private Instant createdAt;

    private PrescriptionMaxDailyDoseWarningLog(
            UUID id,
            UUID prescriptionId,
            UUID patientId,
            String activeIngredient,
            BigDecimal totalDailyDoseMg,
            BigDecimal maxDailyDoseMg,
            String overrideReason,
            UUID handledBy,
            Instant handledAt,
            Instant createdAt
    ) {
        this.id = requireNonNull(id, "Max daily dose warning log id is required.");
        this.prescriptionId = requireNonNull(prescriptionId, "Prescription id is required.");
        this.patientId = requireNonNull(patientId, "Patient id is required.");
        this.activeIngredient = requireText(activeIngredient, "Active ingredient is required.");
        this.totalDailyDoseMg = requirePositive(totalDailyDoseMg, "Total daily dose (mg) is required.");
        this.maxDailyDoseMg = requirePositive(maxDailyDoseMg, "Max daily dose (mg) is required.");
        this.overrideReason = normalizeOptionalText(overrideReason);
        this.handledBy = requireNonNull(handledBy, "Handled by is required.");
        this.handledAt = requireNonNull(handledAt, "Handled at is required.");
        this.createdAt = requireNonNull(createdAt, "Created at is required.");
    }

    public static PrescriptionMaxDailyDoseWarningLog create(
            UUID id,
            UUID prescriptionId,
            UUID patientId,
            String activeIngredient,
            BigDecimal totalDailyDoseMg,
            BigDecimal maxDailyDoseMg,
            String overrideReason,
            UUID handledBy,
            Instant handledAt
    ) {
        return new PrescriptionMaxDailyDoseWarningLog(
                id,
                prescriptionId,
                patientId,
                activeIngredient,
                totalDailyDoseMg,
                maxDailyDoseMg,
                overrideReason,
                handledBy,
                handledAt,
                handledAt
        );
    }

    public static PrescriptionMaxDailyDoseWarningLog restore(
            UUID id,
            UUID prescriptionId,
            UUID patientId,
            String activeIngredient,
            BigDecimal totalDailyDoseMg,
            BigDecimal maxDailyDoseMg,
            String overrideReason,
            UUID handledBy,
            Instant handledAt,
            Instant createdAt
    ) {
        return new PrescriptionMaxDailyDoseWarningLog(
                id,
                prescriptionId,
                patientId,
                activeIngredient,
                totalDailyDoseMg,
                maxDailyDoseMg,
                overrideReason,
                handledBy,
                handledAt,
                createdAt
        );
    }

    private static BigDecimal requirePositive(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(message);
        }
        return value;
    }

    private static <T> T requireNonNull(T value, String message) {
        if (Objects.isNull(value)) {
            throw new ValidationException(message);
        }
        return value;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(message);
        }
        return value.trim();
    }

    private static String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

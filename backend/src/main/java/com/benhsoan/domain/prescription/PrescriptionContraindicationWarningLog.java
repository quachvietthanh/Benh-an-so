package com.benhsoan.domain.prescription;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.contraindication.enums.ContraindicationSeverity;
import com.benhsoan.domain.contraindication.enums.ContraindicationType;
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
public class PrescriptionContraindicationWarningLog {

    private UUID id;
    private UUID prescriptionId;
    private UUID patientId;
    private UUID ruleId;
    private UUID medicineId;
    private ContraindicationType contraindicationType;
    private ContraindicationSeverity severity;
    private String message;
    private String recommendation;
    private String overrideReason;
    private UUID handledBy;
    private Instant handledAt;
    private Instant createdAt;

    private PrescriptionContraindicationWarningLog(
            UUID id,
            UUID prescriptionId,
            UUID patientId,
            UUID ruleId,
            UUID medicineId,
            ContraindicationType contraindicationType,
            ContraindicationSeverity severity,
            String message,
            String recommendation,
            String overrideReason,
            UUID handledBy,
            Instant handledAt,
            Instant createdAt
    ) {
        this.id = requireNonNull(id, "Contraindication warning log id is required.");
        this.prescriptionId = requireNonNull(prescriptionId, "Prescription id is required.");
        this.patientId = requireNonNull(patientId, "Patient id is required.");
        this.ruleId = requireNonNull(ruleId, "Contraindication rule id is required.");
        this.medicineId = requireNonNull(medicineId, "Medicine id is required.");
        this.contraindicationType = requireNonNull(contraindicationType, "Contraindication type is required.");
        this.severity = requireNonNull(severity, "Contraindication severity is required.");
        this.message = requireText(message, "Contraindication warning message is required.");
        this.recommendation = normalizeOptionalText(recommendation);
        this.overrideReason = validateOverrideReason(overrideReason);
        this.handledBy = requireNonNull(handledBy, "Handled by is required.");
        this.handledAt = requireNonNull(handledAt, "Handled at is required.");
        this.createdAt = requireNonNull(createdAt, "Created at is required.");
    }

    public static PrescriptionContraindicationWarningLog create(
            UUID id,
            UUID prescriptionId,
            UUID patientId,
            UUID ruleId,
            UUID medicineId,
            ContraindicationType contraindicationType,
            ContraindicationSeverity severity,
            String message,
            String recommendation,
            String overrideReason,
            UUID handledBy,
            Instant handledAt
    ) {
        return new PrescriptionContraindicationWarningLog(
                id,
                prescriptionId,
                patientId,
                ruleId,
                medicineId,
                contraindicationType,
                severity,
                message,
                recommendation,
                overrideReason,
                handledBy,
                handledAt,
                handledAt
        );
    }

    public static PrescriptionContraindicationWarningLog restore(
            UUID id,
            UUID prescriptionId,
            UUID patientId,
            UUID ruleId,
            UUID medicineId,
            ContraindicationType contraindicationType,
            ContraindicationSeverity severity,
            String message,
            String recommendation,
            String overrideReason,
            UUID handledBy,
            Instant handledAt,
            Instant createdAt
    ) {
        return new PrescriptionContraindicationWarningLog(
                id,
                prescriptionId,
                patientId,
                ruleId,
                medicineId,
                contraindicationType,
                severity,
                message,
                recommendation,
                overrideReason,
                handledBy,
                handledAt,
                createdAt
        );
    }

    public boolean wasOverridden() {
        return overrideReason != null;
    }

    private static String validateOverrideReason(String overrideReason) {
        return normalizeOptionalText(overrideReason);
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

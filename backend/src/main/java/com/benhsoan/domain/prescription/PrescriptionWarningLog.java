package com.benhsoan.domain.prescription;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.druginteraction.enums.InteractionSeverity;
import com.benhsoan.domain.prescription.enums.WarningAction;
import com.benhsoan.domain.shared.exception.ValidationException;
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
public class PrescriptionWarningLog {

    private UUID id;

    private UUID prescriptionId;

    private UUID ruleId;

    private UUID firstMedicineId;

    private UUID secondMedicineId;

    private InteractionSeverity severity;

    private String warningMessage;

    private WarningAction action;

    private String overrideReason;

    private UUID handledBy;

    private Instant handledAt;

    private Instant createdAt;

    private PrescriptionWarningLog(
            UUID id,
            UUID prescriptionId,
            UUID ruleId,
            UUID firstMedicineId,
            UUID secondMedicineId,
            InteractionSeverity severity,
            String warningMessage,
            WarningAction action,
            String overrideReason,
            UUID handledBy,
            Instant handledAt,
            Instant createdAt
    ) {
        this.id = requireNonNull(id, "Prescription warning log id is required.");
        this.prescriptionId = requireNonNull(prescriptionId, "Prescription id is required.");
        this.ruleId = requireNonNull(ruleId, "Drug interaction rule id is required.");
        UUID validatedFirstMedicineId = requireNonNull(firstMedicineId, "First medicine id is required.");
        UUID validatedSecondMedicineId = requireNonNull(secondMedicineId, "Second medicine id is required.");
        validateDifferentMedicines(validatedFirstMedicineId, validatedSecondMedicineId);
        if (validatedFirstMedicineId.compareTo(validatedSecondMedicineId) <= 0) {
            this.firstMedicineId = validatedFirstMedicineId;
            this.secondMedicineId = validatedSecondMedicineId;
        } else {
            this.firstMedicineId = validatedSecondMedicineId;
            this.secondMedicineId = validatedFirstMedicineId;
        }
        this.severity = requireNonNull(severity, "Interaction severity is required.");
        this.warningMessage = requireText(warningMessage, "Warning message is required.");
        this.action = requireNonNull(action, "Warning action is required.");
        this.overrideReason = validateOverrideReason(action, overrideReason);
        this.handledBy = requireNonNull(handledBy, "Warning handler id is required.");
        this.handledAt = requireNonNull(handledAt, "Warning handling time is required.");
        this.createdAt = requireNonNull(createdAt, "Warning log creation time is required.");
    }

    public static PrescriptionWarningLog create(
            UUID id,
            UUID prescriptionId,
            UUID ruleId,
            UUID firstMedicineId,
            UUID secondMedicineId,
            InteractionSeverity severity,
            String warningMessage,
            WarningAction action,
            String overrideReason,
            UUID handledBy,
            Instant handledAt,
            Instant createdAt
    ) {
        return new PrescriptionWarningLog(
                id,
                prescriptionId,
                ruleId,
                firstMedicineId,
                secondMedicineId,
                severity,
                warningMessage,
                action,
                overrideReason,
                handledBy,
                handledAt,
                createdAt
        );
    }

    public static PrescriptionWarningLog restore(
            UUID id,
            UUID prescriptionId,
            UUID ruleId,
            UUID firstMedicineId,
            UUID secondMedicineId,
            InteractionSeverity severity,
            String warningMessage,
            WarningAction action,
            String overrideReason,
            UUID handledBy,
            Instant handledAt,
            Instant createdAt
    ) {
        return new PrescriptionWarningLog(
                id,
                prescriptionId,
                ruleId,
                firstMedicineId,
                secondMedicineId,
                severity,
                warningMessage,
                action,
                overrideReason,
                handledBy,
                handledAt,
                createdAt
        );
    }

    public boolean wasOverridden() {
        return action == WarningAction.OVERRIDDEN;
    }

    private static String validateOverrideReason(WarningAction action, String overrideReason) {
        if (action == WarningAction.OVERRIDDEN
                && (overrideReason == null || overrideReason.isBlank())) {
            throw new ValidationException("Override reason is required when a drug interaction warning is overridden.");
        }
        return normalizeOptionalText(overrideReason);
    }

    private static void validateDifferentMedicines(UUID firstMedicineId, UUID secondMedicineId) {
        if (firstMedicineId.equals(secondMedicineId)) {
            throw new ValidationException("A warning must reference two different medicines.");
        }
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

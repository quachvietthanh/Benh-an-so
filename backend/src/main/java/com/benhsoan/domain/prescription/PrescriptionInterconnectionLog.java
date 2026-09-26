package com.benhsoan.domain.prescription;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.prescription.enums.PrescriptionInterconnectionAttemptType;
import com.benhsoan.domain.prescription.enums.PrescriptionInterconnectionOutcome;
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
public class PrescriptionInterconnectionLog {

    private UUID id;
    private UUID prescriptionId;
    private int attemptNumber;
    private PrescriptionInterconnectionAttemptType attemptType;
    private PrescriptionInterconnectionOutcome outcome;
    private String requestPayload;
    private String responsePayload;
    private String receiptCode;
    private String failureReason;
    private UUID attemptedBy;
    private Instant startedAt;
    private Instant completedAt;

    private PrescriptionInterconnectionLog(
            UUID id,
            UUID prescriptionId,
            int attemptNumber,
            PrescriptionInterconnectionAttemptType attemptType,
            PrescriptionInterconnectionOutcome outcome,
            String requestPayload,
            String responsePayload,
            String receiptCode,
            String failureReason,
            UUID attemptedBy,
            Instant startedAt,
            Instant completedAt
    ) {
        this.id = requireNonNull(id, "Interconnection log id is required.");
        this.prescriptionId = requireNonNull(prescriptionId, "Prescription id is required.");
        this.attemptNumber = requirePositive(attemptNumber, "Attempt number");
        this.attemptType = requireNonNull(attemptType, "Interconnection attempt type is required.");
        this.outcome = requireNonNull(outcome, "Interconnection outcome is required.");
        this.requestPayload = requireText(requestPayload, "Interconnection request payload is required.");
        this.responsePayload = normalizeOptional(responsePayload);
        this.receiptCode = normalizeOptional(receiptCode);
        this.failureReason = normalizeOptional(failureReason);
        this.attemptedBy = requireNonNull(attemptedBy, "Interconnection actor id is required.");
        this.startedAt = requireNonNull(startedAt, "Interconnection start time is required.");
        this.completedAt = requireNonNull(completedAt, "Interconnection completion time is required.");
        validateResult();
        if (completedAt.isBefore(startedAt)) {
            throw new ValidationException("Interconnection completion time must not be before start time.");
        }
    }

    public static PrescriptionInterconnectionLog create(
            UUID id,
            UUID prescriptionId,
            int attemptNumber,
            PrescriptionInterconnectionAttemptType attemptType,
            PrescriptionInterconnectionOutcome outcome,
            String requestPayload,
            String responsePayload,
            String receiptCode,
            String failureReason,
            UUID attemptedBy,
            Instant startedAt,
            Instant completedAt
    ) {
        return new PrescriptionInterconnectionLog(
                id, prescriptionId, attemptNumber, attemptType, outcome,
                requestPayload, responsePayload, receiptCode, failureReason,
                attemptedBy, startedAt, completedAt
        );
    }

    public static PrescriptionInterconnectionLog restore(
            UUID id,
            UUID prescriptionId,
            int attemptNumber,
            PrescriptionInterconnectionAttemptType attemptType,
            PrescriptionInterconnectionOutcome outcome,
            String requestPayload,
            String responsePayload,
            String receiptCode,
            String failureReason,
            UUID attemptedBy,
            Instant startedAt,
            Instant completedAt
    ) {
        return create(id, prescriptionId, attemptNumber, attemptType, outcome,
                requestPayload, responsePayload, receiptCode, failureReason,
                attemptedBy, startedAt, completedAt);
    }

    private void validateResult() {
        if (outcome == PrescriptionInterconnectionOutcome.SUCCESS
                && (receiptCode == null || failureReason != null)) {
            throw new ValidationException("Successful interconnection attempts require a receipt and no failure reason.");
        }
        if (outcome == PrescriptionInterconnectionOutcome.FAILED
                && (receiptCode != null || failureReason == null)) {
            throw new ValidationException("Failed interconnection attempts require a failure reason and no receipt.");
        }
    }

    private static int requirePositive(int value, String field) {
        if (value <= 0) {
            throw new ValidationException(field + " must be greater than zero.");
        }
        return value;
    }

    private static String normalizeOptional(String value) {
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

package com.benhsoan.domain.prescription;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.prescription.exception.PrescriptionAmendmentReasonRequiredException;
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
public class PrescriptionAmendment {

    private UUID id;

    private UUID prescriptionId;

    private String changeReason;

    private String beforeData;

    private String afterData;

    private UUID amendedBy;

    private Instant amendedAt;

    private PrescriptionAmendment(
            UUID id,
            UUID prescriptionId,
            String changeReason,
            String beforeData,
            String afterData,
            UUID amendedBy,
            Instant amendedAt
    ) {
        this.id = requireNonNull(id, "Prescription amendment id is required.");
        this.prescriptionId = requireNonNull(prescriptionId, "Prescription id is required.");
        this.changeReason = validateChangeReason(changeReason);
        this.beforeData = requireText(beforeData, "Prescription data before amendment is required.");
        this.afterData = requireText(afterData, "Prescription data after amendment is required.");
        this.amendedBy = requireNonNull(amendedBy, "Amending doctor id is required.");
        this.amendedAt = requireNonNull(amendedAt, "Prescription amendment time is required.");
    }

    public static PrescriptionAmendment create(
            UUID id,
            UUID prescriptionId,
            String changeReason,
            String beforeData,
            String afterData,
            UUID amendedBy,
            Instant amendedAt
    ) {
        return new PrescriptionAmendment(
                id,
                prescriptionId,
                changeReason,
                beforeData,
                afterData,
                amendedBy,
                amendedAt
        );
    }

    public static PrescriptionAmendment restore(
            UUID id,
            UUID prescriptionId,
            String changeReason,
            String beforeData,
            String afterData,
            UUID amendedBy,
            Instant amendedAt
    ) {
        return new PrescriptionAmendment(
                id,
                prescriptionId,
                changeReason,
                beforeData,
                afterData,
                amendedBy,
                amendedAt
        );
    }

    private static String validateChangeReason(String changeReason) {
        if (changeReason == null || changeReason.isBlank()) {
            throw new PrescriptionAmendmentReasonRequiredException();
        }
        return changeReason.trim();
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

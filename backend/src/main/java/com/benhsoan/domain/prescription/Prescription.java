package com.benhsoan.domain.prescription;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
import com.benhsoan.domain.prescription.enums.InterconnectionStatus;
import com.benhsoan.domain.prescription.exception.PrescriptionAlreadyCancelledException;
import com.benhsoan.domain.prescription.exception.PrescriptionAlreadyDispensedException;
import com.benhsoan.domain.prescription.exception.PrescriptionInvalidStatusException;
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
public class Prescription {

    private UUID id;

    private String prescriptionCode;

    private UUID medicalRecordId;

    private PrescriptionStatus status;

    private String note;

    private UUID prescribedBy;

    private Instant prescribedAt;

    private UUID updatedBy;

    private Instant updatedAt;

    private InterconnectionStatus interconnectionStatus;

    private Instant lastInterconnectionAt;

    private String lastInterconnectionError;

    private String interconnectionReceiptCode;

    private List<PrescriptionItem> items;

    private Prescription(
            UUID id,
            String prescriptionCode,
            UUID medicalRecordId,
            PrescriptionStatus status,
            String note,
            UUID prescribedBy,
            Instant prescribedAt,
            UUID updatedBy,
            Instant updatedAt,
            InterconnectionStatus interconnectionStatus,
            Instant lastInterconnectionAt,
            String lastInterconnectionError,
            String interconnectionReceiptCode,
            List<PrescriptionItem> items
    ) {
        this.id = requireNonNull(id, "Prescription id is required.");
        this.prescriptionCode = requireText(prescriptionCode, "Prescription code is required.");
        this.medicalRecordId = requireNonNull(medicalRecordId, "Medical record id is required.");
        this.status = requireNonNull(status, "Prescription status is required.");
        this.note = normalizeOptionalText(note);
        this.prescribedBy = requireNonNull(prescribedBy, "Prescribing doctor id is required.");
        this.prescribedAt = requireNonNull(prescribedAt, "Prescription time is required.");
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
        this.interconnectionStatus = requireNonNull(interconnectionStatus, "Interconnection status is required.");
        this.lastInterconnectionAt = lastInterconnectionAt;
        this.lastInterconnectionError = normalizeOptionalText(lastInterconnectionError);
        this.interconnectionReceiptCode = normalizeOptionalText(interconnectionReceiptCode);
        validateInterconnectionState();
        this.items = validateAndCopyItems(items, id);
    }

    public static Prescription create(
            UUID id,
            String prescriptionCode,
            UUID medicalRecordId,
            String note,
            UUID prescribedBy,
            Instant prescribedAt,
            List<PrescriptionItem> items
    ) {
        return new Prescription(
                id,
                prescriptionCode,
                medicalRecordId,
                PrescriptionStatus.PENDING_DISPENSE,
                note,
                prescribedBy,
                prescribedAt,
                null,
                null,
                InterconnectionStatus.NOT_SENT,
                null,
                null,
                null,
                items
        );
    }

    public static Prescription restore(
            UUID id,
            String prescriptionCode,
            UUID medicalRecordId,
            PrescriptionStatus status,
            String note,
            UUID prescribedBy,
            Instant prescribedAt,
            UUID updatedBy,
            Instant updatedAt,
            List<PrescriptionItem> items
    ) {
        return restore(
                id, prescriptionCode, medicalRecordId, status, note, prescribedBy, prescribedAt,
                updatedBy, updatedAt, InterconnectionStatus.NOT_SENT, null, null, null, items
        );
    }

    public static Prescription restore(
            UUID id,
            String prescriptionCode,
            UUID medicalRecordId,
            PrescriptionStatus status,
            String note,
            UUID prescribedBy,
            Instant prescribedAt,
            UUID updatedBy,
            Instant updatedAt,
            InterconnectionStatus interconnectionStatus,
            Instant lastInterconnectionAt,
            String lastInterconnectionError,
            String interconnectionReceiptCode,
            List<PrescriptionItem> items
    ) {
        return new Prescription(
                id,
                prescriptionCode,
                medicalRecordId,
                status,
                note,
                prescribedBy,
                prescribedAt,
                updatedBy,
                updatedAt,
                interconnectionStatus,
                lastInterconnectionAt,
                lastInterconnectionError,
                interconnectionReceiptCode,
                items
        );
    }

    public void markInterconnectionSucceeded(String receiptCode, Instant completedAt) {
        ensureNotInterconnected();
        this.interconnectionStatus = InterconnectionStatus.SUCCESS;
        this.lastInterconnectionAt = requireNonNull(completedAt, "Interconnection completion time is required.");
        this.lastInterconnectionError = null;
        this.interconnectionReceiptCode = requireText(receiptCode, "Interconnection receipt code is required.");
    }

    public void markInterconnectionFailed(String failureReason, Instant completedAt) {
        ensureNotInterconnected();
        this.interconnectionStatus = InterconnectionStatus.FAILED;
        this.lastInterconnectionAt = requireNonNull(completedAt, "Interconnection completion time is required.");
        this.lastInterconnectionError = requireText(failureReason, "Interconnection failure reason is required.");
        this.interconnectionReceiptCode = null;
    }

    public void replaceItems(
            List<PrescriptionItem> replacementItems,
            String note,
            UUID updatedBy,
            Instant updatedAt
    ) {
        ensurePendingDispense("Only pending prescriptions can be amended.");
        List<PrescriptionItem> validatedItems = validateAndCopyItems(replacementItems, id);
        String normalizedNote = normalizeOptionalText(note);
        UUID validatedUpdatedBy = requireNonNull(updatedBy, "Prescription updater id is required.");
        Instant validatedUpdatedAt = requireNonNull(updatedAt, "Prescription update time is required.");

        this.items = validatedItems;
        this.note = normalizedNote;
        this.updatedBy = validatedUpdatedBy;
        this.updatedAt = validatedUpdatedAt;
    }

    public void markDispensed(UUID dispensedBy, Instant dispensedAt) {
        if (status == PrescriptionStatus.DISPENSED) {
            throw new PrescriptionAlreadyDispensedException();
        }
        if (status == PrescriptionStatus.CANCELLED) {
            throw new PrescriptionInvalidStatusException("Cancelled prescriptions cannot be dispensed.");
        }

        UUID validatedDispensedBy = requireNonNull(dispensedBy, "Dispensing user id is required.");
        Instant validatedDispensedAt = requireNonNull(dispensedAt, "Dispensing time is required.");
        this.status = PrescriptionStatus.DISPENSED;
        this.updatedBy = validatedDispensedBy;
        this.updatedAt = validatedDispensedAt;
    }

    public void cancel(UUID cancelledBy, Instant cancelledAt) {
        if (status == PrescriptionStatus.CANCELLED) {
            throw new PrescriptionAlreadyCancelledException();
        }
        if (status == PrescriptionStatus.DISPENSED) {
            throw new PrescriptionAlreadyDispensedException();
        }

        UUID validatedCancelledBy = requireNonNull(cancelledBy, "Cancelling user id is required.");
        Instant validatedCancelledAt = requireNonNull(cancelledAt, "Cancellation time is required.");
        this.status = PrescriptionStatus.CANCELLED;
        this.updatedBy = validatedCancelledBy;
        this.updatedAt = validatedCancelledAt;
    }

    public boolean isPendingDispense() {
        return status == PrescriptionStatus.PENDING_DISPENSE;
    }

    private void ensurePendingDispense(String message) {
        if (!isPendingDispense()) {
            throw new PrescriptionInvalidStatusException(message);
        }
    }

    private void ensureNotInterconnected() {
        if (interconnectionStatus == InterconnectionStatus.SUCCESS) {
            throw new PrescriptionInvalidStatusException(
                    "Successfully interconnected prescriptions cannot be submitted again."
            );
        }
    }

    private void validateInterconnectionState() {
        boolean valid = switch (interconnectionStatus) {
            case NOT_SENT -> lastInterconnectionAt == null
                    && lastInterconnectionError == null
                    && interconnectionReceiptCode == null;
            case SUCCESS -> lastInterconnectionAt != null
                    && lastInterconnectionError == null
                    && interconnectionReceiptCode != null;
            case FAILED -> lastInterconnectionAt != null
                    && lastInterconnectionError != null
                    && interconnectionReceiptCode == null;
        };
        if (!valid) {
            throw new ValidationException("Prescription interconnection state is inconsistent.");
        }
    }

    private static List<PrescriptionItem> validateAndCopyItems(
            List<PrescriptionItem> items,
            UUID prescriptionId
    ) {
        if (items == null || items.isEmpty()) {
            throw new ValidationException("Prescription must contain at least one medicine.");
        }

        Set<UUID> medicineIds = new HashSet<>();
        for (PrescriptionItem item : items) {
            if (item == null) {
                throw new ValidationException("Prescription item is required.");
            }
            if (!prescriptionId.equals(item.getPrescriptionId())) {
                throw new ValidationException(
                        "Prescription item does not belong to prescription: " + prescriptionId
                );
            }
            if (!medicineIds.add(item.getMedicineId())) {
                throw new ValidationException("Medicine already exists in the prescription: " + item.getMedicineId());
            }
        }

        return List.copyOf(items);
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

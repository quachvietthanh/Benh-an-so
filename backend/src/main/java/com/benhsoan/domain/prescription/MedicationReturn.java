package com.benhsoan.domain.prescription;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

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
public class MedicationReturn {

    private UUID id;
    private UUID prescriptionId;
    private UUID prescriptionItemId;
    private UUID dispenseItemId;
    private UUID medicineId;
    private UUID medicineBatchId;
    private int returnedQuantity;
    private String reason;
    private UUID returnedBy;
    private Instant returnedAt;
    private Instant createdAt;

    private MedicationReturn(
            UUID id,
            UUID prescriptionId,
            UUID prescriptionItemId,
            UUID dispenseItemId,
            UUID medicineId,
            UUID medicineBatchId,
            int returnedQuantity,
            String reason,
            UUID returnedBy,
            Instant returnedAt,
            Instant createdAt
    ) {
        this.id = requireNonNull(id, "Medication return id is required.");
        this.prescriptionId = requireNonNull(prescriptionId, "Prescription id is required.");
        this.prescriptionItemId = requireNonNull(prescriptionItemId, "Prescription item id is required.");
        this.dispenseItemId = requireNonNull(dispenseItemId, "Dispense item id is required.");
        this.medicineId = requireNonNull(medicineId, "Medicine id is required.");
        this.medicineBatchId = requireNonNull(medicineBatchId, "Medicine batch id is required.");
        this.returnedQuantity = validateReturnedQuantity(returnedQuantity);
        this.reason = requireText(reason, "Return reason is required.");
        this.returnedBy = requireNonNull(returnedBy, "Returning user id is required.");
        this.returnedAt = requireNonNull(returnedAt, "Return time is required.");
        this.createdAt = requireNonNull(createdAt, "Return creation time is required.");
    }

    public static MedicationReturn create(
            UUID id,
            UUID prescriptionId,
            UUID prescriptionItemId,
            UUID dispenseItemId,
            UUID medicineId,
            UUID medicineBatchId,
            int returnedQuantity,
            String reason,
            UUID returnedBy,
            Instant returnedAt
    ) {
        return new MedicationReturn(
                id,
                prescriptionId,
                prescriptionItemId,
                dispenseItemId,
                medicineId,
                medicineBatchId,
                returnedQuantity,
                reason,
                returnedBy,
                returnedAt,
                returnedAt
        );
    }

    public static MedicationReturn restore(
            UUID id,
            UUID prescriptionId,
            UUID prescriptionItemId,
            UUID dispenseItemId,
            UUID medicineId,
            UUID medicineBatchId,
            int returnedQuantity,
            String reason,
            UUID returnedBy,
            Instant returnedAt,
            Instant createdAt
    ) {
        return new MedicationReturn(
                id,
                prescriptionId,
                prescriptionItemId,
                dispenseItemId,
                medicineId,
                medicineBatchId,
                returnedQuantity,
                reason,
                returnedBy,
                returnedAt,
                createdAt
        );
    }

    private static int validateReturnedQuantity(int returnedQuantity) {
        if (returnedQuantity <= 0) {
            throw new ValidationException("Returned quantity must be greater than zero.");
        }
        return returnedQuantity;
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

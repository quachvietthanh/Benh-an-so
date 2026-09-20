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
public class PrescriptionDispenseItem {

    private UUID id;
    private UUID prescriptionId;
    private UUID prescriptionItemId;
    private UUID medicineId;
    private UUID medicineBatchId;
    private int dispensedQuantity;
    private int returnedQuantity;
    private UUID dispensedBy;
    private Instant dispensedAt;
    private Instant createdAt;

    private PrescriptionDispenseItem(
            UUID id,
            UUID prescriptionId,
            UUID prescriptionItemId,
            UUID medicineId,
            UUID medicineBatchId,
            int dispensedQuantity,
            int returnedQuantity,
            UUID dispensedBy,
            Instant dispensedAt,
            Instant createdAt
    ) {
        this.id = requireNonNull(id, "Prescription dispense item id is required.");
        this.prescriptionId = requireNonNull(prescriptionId, "Prescription id is required.");
        this.prescriptionItemId = requireNonNull(prescriptionItemId, "Prescription item id is required.");
        this.medicineId = requireNonNull(medicineId, "Medicine id is required.");
        this.medicineBatchId = requireNonNull(medicineBatchId, "Medicine batch id is required.");
        this.dispensedQuantity = requirePositive(dispensedQuantity, "Dispensed quantity must be greater than 0.");
        this.returnedQuantity = validateReturnedQuantity(returnedQuantity, this.dispensedQuantity);
        this.dispensedBy = requireNonNull(dispensedBy, "Dispensed by is required.");
        this.dispensedAt = requireNonNull(dispensedAt, "Dispensed at is required.");
        this.createdAt = requireNonNull(createdAt, "Created at is required.");
    }

    public static PrescriptionDispenseItem create(
            UUID id,
            UUID prescriptionId,
            UUID prescriptionItemId,
            UUID medicineId,
            UUID medicineBatchId,
            int dispensedQuantity,
            UUID dispensedBy,
            Instant dispensedAt
    ) {
        return new PrescriptionDispenseItem(
                id,
                prescriptionId,
                prescriptionItemId,
                medicineId,
                medicineBatchId,
                dispensedQuantity,
                0,
                dispensedBy,
                dispensedAt,
                dispensedAt
        );
    }

    public static PrescriptionDispenseItem restore(
            UUID id,
            UUID prescriptionId,
            UUID prescriptionItemId,
            UUID medicineId,
            UUID medicineBatchId,
            int dispensedQuantity,
            int returnedQuantity,
            UUID dispensedBy,
            Instant dispensedAt,
            Instant createdAt
    ) {
        return new PrescriptionDispenseItem(
                id,
                prescriptionId,
                prescriptionItemId,
                medicineId,
                medicineBatchId,
                dispensedQuantity,
                returnedQuantity,
                dispensedBy,
                dispensedAt,
                createdAt
        );
    }

    public void recordReturn(int quantity) {
        if (quantity <= 0) {
            throw new ValidationException("Returned quantity must be greater than zero.");
        }
        int returnable = this.dispensedQuantity - this.returnedQuantity;
        if (quantity > returnable) {
            throw new ValidationException("Returned quantity exceeds the remaining returnable quantity.");
        }
        this.returnedQuantity += quantity;
    }

    public int getRemainingReturnableQuantity() {
        return this.dispensedQuantity - this.returnedQuantity;
    }

    private static int validateReturnedQuantity(int returnedQuantity, int dispensedQuantity) {
        if (returnedQuantity < 0) {
            throw new ValidationException("Returned quantity must not be negative.");
        }
        if (returnedQuantity > dispensedQuantity) {
            throw new ValidationException("Returned quantity cannot exceed the dispensed quantity.");
        }
        return returnedQuantity;
    }

    private static int requirePositive(int value, String message) {
        if (value <= 0) {
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
}

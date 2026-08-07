package com.benhsoan.domain.inventory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.inventory.enums.BatchStatus;
import com.benhsoan.domain.shared.Guard.Guard;
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
public class MedicineBatch {

    private UUID id;
    private UUID medicineId;
    private String batchNumber;
    private LocalDate expiryDate;
    private int quantity;
    private BatchStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    private MedicineBatch(
            UUID id,
            UUID medicineId,
            String batchNumber,
            LocalDate expiryDate,
            int quantity,
            BatchStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Guard.require(id, "Medicine batch id");
        this.medicineId = Guard.require(medicineId, "Medicine id");
        this.batchNumber = Guard.require(batchNumber, "Batch number");
        this.expiryDate = Guard.require(expiryDate, "Expiry date");
        this.quantity = quantity;
        this.status = Guard.require(status, "Batch status");
        this.createdAt = Guard.require(createdAt, "Creation time");
        this.updatedAt = updatedAt;
    }

    public static MedicineBatch create(
            UUID id,
            UUID medicineId,
            String batchNumber,
            LocalDate expiryDate,
            Instant createdAt
    ) {
        return new MedicineBatch(
                id, medicineId, batchNumber, expiryDate,
                0, BatchStatus.ACTIVE, createdAt, null
        );
    }

    public static MedicineBatch restore(
            UUID id,
            UUID medicineId,
            String batchNumber,
            LocalDate expiryDate,
            int quantity,
            BatchStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new MedicineBatch(
                id, medicineId, batchNumber, expiryDate,
                quantity, status, createdAt, updatedAt
        );
    }

    public void addStock(int additionalQuantity, Instant updatedAt) {
        if (additionalQuantity <= 0) {
            throw new ValidationException("Additional quantity must be greater than 0.");
        }
        this.quantity += additionalQuantity;
        this.status = BatchStatus.ACTIVE;
        this.updatedAt = Guard.require(updatedAt, "Update time");
    }

    public void deductStock(int deductionQuantity, Instant updatedAt) {
        if (deductionQuantity <= 0) {
            throw new ValidationException("Deduction quantity must be greater than 0.");
        }
        if (deductionQuantity > quantity) {
            throw new ValidationException("Deduction quantity exceeds available batch stock.");
        }

        this.quantity -= deductionQuantity;
        this.status = this.quantity == 0 ? BatchStatus.DEPLETED : BatchStatus.ACTIVE;
        this.updatedAt = Guard.require(updatedAt, "Update time");
    }

    public boolean isEligibleForDispenseOn(LocalDate today) {
        LocalDate validatedToday = Guard.require(today, "Today");
        return status == BatchStatus.ACTIVE
                && quantity > 0
                && !expiryDate.isBefore(validatedToday);
    }
}

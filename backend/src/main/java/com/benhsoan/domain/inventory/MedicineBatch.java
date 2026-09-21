package com.benhsoan.domain.inventory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.inventory.enums.BatchStatus;
import com.benhsoan.domain.inventory.exception.BatchAlreadyDiscardedException;
import com.benhsoan.domain.inventory.exception.BatchNotExpiredException;
import com.benhsoan.domain.inventory.exception.BatchStateConflictException;
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

    public boolean isExpiredOn(LocalDate today) {
        LocalDate validatedToday = Guard.require(today, "Today");
        return status == BatchStatus.ACTIVE
                && quantity > 0
                && expiryDate.isBefore(validatedToday);
    }

    public boolean isNearExpiryOn(LocalDate today, long alertDays) {
        LocalDate validatedToday = Guard.require(today, "Today");
        if (alertDays < 0) {
            throw new ValidationException("Expiry alert days must not be negative.");
        }
        return status == BatchStatus.ACTIVE
                && quantity > 0
                && !expiryDate.isBefore(validatedToday)
                && !expiryDate.isAfter(validatedToday.plusDays(alertDays));
    }

    public void adjustStock(int actualQuantity, LocalDate today, Instant updatedAt) {
        LocalDate validatedToday = Guard.require(today, "Today");
        if (actualQuantity < 0) {
            throw new ValidationException("Số lượng tồn kho thực tế không được âm.");
        }
        if (this.status == BatchStatus.EXPIRED) {
            throw new BatchStateConflictException(this.id, "Không thể điều chỉnh tồn kho cho lô thuốc đã bị hủy hoặc hết hạn.");
        }
        if (this.expiryDate.isBefore(validatedToday)) {
            throw new BatchStateConflictException(
                    this.id,
                    "Không thể điều chỉnh tồn kho cho lô thuốc đã hết hạn sử dụng. Vui lòng thực hiện quy trình hủy lô."
            );
        }
        if (actualQuantity == this.quantity) {
            throw new ValidationException(
                    "Số lượng kiểm kê thực tế trùng khớp với tồn kho hiện tại, không có chênh lệch để điều chỉnh."
            );
        }
        this.quantity = actualQuantity;
        this.status = this.quantity == 0 ? BatchStatus.DEPLETED : BatchStatus.ACTIVE;
        this.updatedAt = Guard.require(updatedAt, "Update time");
    }


    public void discardExpired(LocalDate today, Instant updatedAt) {
        LocalDate validatedToday = Guard.require(today, "Today");
        if (this.status == BatchStatus.EXPIRED || this.quantity <= 0) {
            throw new BatchAlreadyDiscardedException(this.id);
        }
        if (!this.expiryDate.isBefore(validatedToday)) {
            throw new BatchNotExpiredException(this.id, this.expiryDate);
        }
        this.quantity = 0;
        this.status = BatchStatus.EXPIRED;
        this.updatedAt = Guard.require(updatedAt, "Update time");
    }
}

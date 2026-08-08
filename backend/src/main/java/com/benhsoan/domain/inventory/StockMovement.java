package com.benhsoan.domain.inventory;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.inventory.enums.StockMovementReferenceType;
import com.benhsoan.domain.inventory.enums.StockMovementType;
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
public class StockMovement {

    private UUID id;
    private UUID medicineId;
    private UUID medicineBatchId;
    private StockMovementType movementType;
    private StockMovementReferenceType referenceType;
    private UUID referenceId;
    private int quantityChange;
    private int quantityBefore;
    private int quantityAfter;
    private UUID performedBy;
    private Instant performedAt;
    private String note;
    private Instant createdAt;

    private StockMovement(
            UUID id,
            UUID medicineId,
            UUID medicineBatchId,
            StockMovementType movementType,
            StockMovementReferenceType referenceType,
            UUID referenceId,
            int quantityChange,
            int quantityBefore,
            int quantityAfter,
            UUID performedBy,
            Instant performedAt,
            String note,
            Instant createdAt
    ) {
        this.id = requireNonNull(id, "Stock movement id is required.");
        this.medicineId = requireNonNull(medicineId, "Medicine id is required.");
        this.medicineBatchId = requireNonNull(medicineBatchId, "Medicine batch id is required.");
        this.movementType = requireNonNull(movementType, "Movement type is required.");
        this.referenceType = requireNonNull(referenceType, "Reference type is required.");
        this.referenceId = requireNonNull(referenceId, "Reference id is required.");
        this.quantityChange = validateNonZero(quantityChange);
        this.quantityBefore = validateNonNegative(quantityBefore, "Quantity before must be non-negative.");
        this.quantityAfter = validateNonNegative(quantityAfter, "Quantity after must be non-negative.");
        validateBalance(quantityBefore, quantityChange, quantityAfter);
        this.performedBy = requireNonNull(performedBy, "Performed by is required.");
        this.performedAt = requireNonNull(performedAt, "Performed at is required.");
        this.note = normalizeOptionalText(note);
        this.createdAt = requireNonNull(createdAt, "Created at is required.");
    }

    public static StockMovement create(
            UUID id,
            UUID medicineId,
            UUID medicineBatchId,
            StockMovementType movementType,
            StockMovementReferenceType referenceType,
            UUID referenceId,
            int quantityChange,
            int quantityBefore,
            int quantityAfter,
            UUID performedBy,
            Instant performedAt,
            String note
    ) {
        return new StockMovement(
                id,
                medicineId,
                medicineBatchId,
                movementType,
                referenceType,
                referenceId,
                quantityChange,
                quantityBefore,
                quantityAfter,
                performedBy,
                performedAt,
                note,
                performedAt
        );
    }

    public static StockMovement restore(
            UUID id,
            UUID medicineId,
            UUID medicineBatchId,
            StockMovementType movementType,
            StockMovementReferenceType referenceType,
            UUID referenceId,
            int quantityChange,
            int quantityBefore,
            int quantityAfter,
            UUID performedBy,
            Instant performedAt,
            String note,
            Instant createdAt
    ) {
        return new StockMovement(
                id,
                medicineId,
                medicineBatchId,
                movementType,
                referenceType,
                referenceId,
                quantityChange,
                quantityBefore,
                quantityAfter,
                performedBy,
                performedAt,
                note,
                createdAt
        );
    }

    private static int validateNonZero(int value) {
        if (value == 0) {
            throw new ValidationException("Quantity change must not be zero.");
        }
        return value;
    }

    private static int validateNonNegative(int value, String message) {
        if (value < 0) {
            throw new ValidationException(message);
        }
        return value;
    }

    private static void validateBalance(int before, int change, int after) {
        if (after != before + change) {
            throw new ValidationException("Stock movement quantity balance is invalid.");
        }
    }

    private static String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static <T> T requireNonNull(T value, String message) {
        if (Objects.isNull(value)) {
            throw new ValidationException(message);
        }
        return value;
    }
}

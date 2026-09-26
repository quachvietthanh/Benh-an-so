package com.benhsoan.domain.inventory;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.inventory.enums.InventoryAlertType;
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
public class InventoryAlertLog {

    private UUID id;
    private UUID medicineId;
    private InventoryAlertType alertType;
    private int thresholdValue;
    private int observedQuantity;
    private Instant createdAt;
    private Instant resolvedAt;

    private InventoryAlertLog(
            UUID id,
            UUID medicineId,
            InventoryAlertType alertType,
            int thresholdValue,
            int observedQuantity,
            Instant createdAt,
            Instant resolvedAt
    ) {
        this.id = requireNonNull(id, "Inventory alert log id is required.");
        this.medicineId = requireNonNull(medicineId, "Medicine id is required.");
        this.alertType = requireNonNull(alertType, "Inventory alert type is required.");
        this.thresholdValue = requireNonNegative(thresholdValue, "Threshold value must not be negative.");
        this.observedQuantity = requireNonNegative(observedQuantity, "Observed quantity must not be negative.");
        this.createdAt = requireNonNull(createdAt, "Created at is required.");
        this.resolvedAt = resolvedAt;
    }

    public static InventoryAlertLog createLowStock(
            UUID medicineId,
            int thresholdValue,
            int observedQuantity,
            Instant createdAt
    ) {
        return new InventoryAlertLog(
                UUID.randomUUID(),
                medicineId,
                InventoryAlertType.LOW_STOCK,
                thresholdValue,
                observedQuantity,
                createdAt,
                null
        );
    }

    public static InventoryAlertLog restore(
            UUID id,
            UUID medicineId,
            InventoryAlertType alertType,
            int thresholdValue,
            int observedQuantity,
            Instant createdAt,
            Instant resolvedAt
    ) {
        return new InventoryAlertLog(
                id,
                medicineId,
                alertType,
                thresholdValue,
                observedQuantity,
                createdAt,
                resolvedAt
        );
    }

    public boolean isResolved() {
        return resolvedAt != null;
    }

    public void resolve(Instant resolvedAt) {
        if (isResolved()) {
            return;
        }
        this.resolvedAt = requireNonNull(resolvedAt, "Resolved at is required.");
    }

    private static <T> T requireNonNull(T value, String message) {
        if (value == null) {
            throw new ValidationException(message);
        }
        return value;
    }

    private static int requireNonNegative(int value, String message) {
        if (value < 0) {
            throw new ValidationException(message);
        }
        return value;
    }
}

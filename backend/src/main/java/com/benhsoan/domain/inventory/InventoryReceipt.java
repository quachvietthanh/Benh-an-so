package com.benhsoan.domain.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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
public class InventoryReceipt {

    private UUID id;
    private UUID receivedBy;
    private Instant receivedAt;
    private String note;
    private Instant createdAt;
    private List<InventoryReceiptItem> items;

    private InventoryReceipt(
            UUID id,
            UUID receivedBy,
            Instant receivedAt,
            String note,
            Instant createdAt,
            List<InventoryReceiptItem> items
    ) {
        this.id = Guard.require(id, "Inventory receipt id");
        this.receivedBy = Guard.require(receivedBy, "Received by user id");
        this.receivedAt = Guard.require(receivedAt, "Received at");
        this.note = note;
        this.createdAt = Guard.require(createdAt, "Creation time");
        this.items = Collections.unmodifiableList(
                new ArrayList<>(Guard.require(items, "Receipt items"))
        );
    }

    public static InventoryReceipt create(
            UUID id,
            UUID receivedBy,
            Instant receivedAt,
            String note,
            Instant createdAt,
            List<InventoryReceiptItem> items
    ) {
        if (items.isEmpty()) {
            throw new ValidationException("Receipt must contain at least one item.");
        }
        return new InventoryReceipt(id, receivedBy, receivedAt, note, createdAt, items);
    }

    @Getter
    @ToString
    @EqualsAndHashCode(of = "id")
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class InventoryReceiptItem {

        private UUID id;
        private UUID inventoryReceiptId;
        private UUID medicineId;
        private UUID medicineBatchId;
        private int quantity;
        private BigDecimal importPrice;
        private BigDecimal totalValue;
        private Instant createdAt;

        private InventoryReceiptItem(
                UUID id,
                UUID inventoryReceiptId,
                UUID medicineId,
                UUID medicineBatchId,
                int quantity,
                BigDecimal importPrice,
                BigDecimal totalValue,
                Instant createdAt
        ) {
            this.id = Guard.require(id, "Receipt item id");
            this.inventoryReceiptId = Guard.require(inventoryReceiptId, "Inventory receipt id");
            this.medicineId = Guard.require(medicineId, "Medicine id");
            this.medicineBatchId = Guard.require(medicineBatchId, "Medicine batch id");
            this.quantity = quantity;
            this.importPrice = Guard.require(importPrice, "Import price");
            this.totalValue = Guard.require(totalValue, "Total value");
            this.createdAt = Guard.require(createdAt, "Creation time");
        }

        public static InventoryReceiptItem create(
                UUID id,
                UUID inventoryReceiptId,
                UUID medicineId,
                UUID medicineBatchId,
                int quantity,
                BigDecimal importPrice,
                Instant createdAt
        ) {
            if (quantity <= 0) {
                throw new ValidationException("Receipt item quantity must be greater than 0.");
            }
            if (importPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("Import price must be non-negative.");
            }
            BigDecimal totalValue = importPrice.multiply(BigDecimal.valueOf(quantity));
            return new InventoryReceiptItem(
                    id, inventoryReceiptId, medicineId, medicineBatchId,
                    quantity, importPrice, totalValue, createdAt
            );
        }
    }
}

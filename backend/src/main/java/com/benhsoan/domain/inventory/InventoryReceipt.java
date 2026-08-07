package com.benhsoan.domain.inventory;

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
}

package com.benhsoan.persistence.entity.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inventory_receipt_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReceiptItemEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "inventory_receipt_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID inventoryReceiptId;

    @Column(name = "medicine_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID medicineId;

    @Column(name = "medicine_batch_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID medicineBatchId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "import_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal importPrice;

    @Column(name = "total_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalValue;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

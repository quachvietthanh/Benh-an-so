package com.benhsoan.persistence.entity.inventory;

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
@Table(name = "medication_procurement_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicationProcurementItemEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "plan_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID planId;

    @Column(name = "medicine_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID medicineId;

    @Column(name = "current_stock", nullable = false)
    private int currentStock;

    @Column(name = "min_stock_threshold", nullable = false)
    private int minStockThreshold;

    @Column(name = "previous_period_consumption", nullable = false)
    private int previousPeriodConsumption;

    @Column(name = "suggested_quantity", nullable = false)
    private int suggestedQuantity;

    @Column(name = "proposed_quantity", nullable = false)
    private int proposedQuantity;

    @Column(name = "approved_quantity", nullable = false)
    private int approvedQuantity;

    @Column(name = "note", length = 255)
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

package com.benhsoan.persistence.entity.inventory;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.inventory.enums.InventoryAlertType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inventory_alert_logs")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryAlertLogEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "medicine_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID medicineId;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 30)
    private InventoryAlertType alertType;

    @Column(name = "threshold_value", nullable = false)
    private int thresholdValue;

    @Column(name = "observed_quantity", nullable = false)
    private int observedQuantity;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}

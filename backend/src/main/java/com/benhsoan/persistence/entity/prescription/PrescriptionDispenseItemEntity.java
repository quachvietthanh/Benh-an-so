package com.benhsoan.persistence.entity.prescription;

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
@Table(name = "prescription_dispense_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionDispenseItemEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "prescription_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID prescriptionId;

    @Column(name = "prescription_item_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID prescriptionItemId;

    @Column(name = "medicine_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID medicineId;

    @Column(name = "medicine_batch_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID medicineBatchId;

    @Column(name = "dispensed_quantity", nullable = false)
    private int dispensedQuantity;

    @Column(name = "dispensed_by", nullable = false, columnDefinition = "BINARY(16)")
    private UUID dispensedBy;

    @Column(name = "dispensed_at", nullable = false)
    private Instant dispensedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

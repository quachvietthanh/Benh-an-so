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
@Table(name = "medication_returns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicationReturnEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "prescription_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID prescriptionId;

    @Column(name = "prescription_item_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID prescriptionItemId;

    @Column(name = "dispense_item_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID dispenseItemId;

    @Column(name = "medicine_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID medicineId;

    @Column(name = "medicine_batch_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID medicineBatchId;

    @Column(name = "returned_quantity", nullable = false)
    private int returnedQuantity;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "returned_by", nullable = false, columnDefinition = "BINARY(16)")
    private UUID returnedBy;

    @Column(name = "returned_at", nullable = false)
    private Instant returnedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

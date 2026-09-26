package com.benhsoan.persistence.entity.controlledmedicine;

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
@Table(name = "controlled_medicine_registers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ControlledMedicineRegisterEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "prescription_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID prescriptionId;

    @Column(name = "prescription_item_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID prescriptionItemId;

    @Column(name = "medicine_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID medicineId;

    @Column(name = "medicine_name", nullable = false, length = 150)
    private String medicineName;

    @Column(name = "patient_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID patientId;

    @Column(name = "prescribed_by", nullable = false, columnDefinition = "BINARY(16)")
    private UUID prescribedBy;

    @Column(name = "dispensed_by", nullable = false, columnDefinition = "BINARY(16)")
    private UUID dispensedBy;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "dispensed_at", nullable = false)
    private Instant dispensedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

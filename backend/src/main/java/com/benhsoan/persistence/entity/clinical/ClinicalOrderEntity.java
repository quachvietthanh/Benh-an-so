package com.benhsoan.persistence.entity.clinical;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.clinical.enums.ClinicalOrderStatus;

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
import lombok.Setter;

@Entity
@Table(name = "clinical_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicalOrderEntity {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    UUID id;
    @Column(name = "order_code", nullable = false, length = 30)
    String orderCode;
    @Column(name = "visit_id", nullable = false, columnDefinition = "BINARY(16)")
    UUID visitId;
    @Column(name = "medical_record_id", nullable = false, columnDefinition = "BINARY(16)")
    UUID medicalRecordId;
    @Column(name = "patient_id", nullable = false, columnDefinition = "BINARY(16)")
    UUID patientId;
    @Column(name = "ordered_by", nullable = false, columnDefinition = "BINARY(16)")
    UUID orderedBy;
    @Column(name = "clinical_reason", columnDefinition = "TEXT")
    String clinicalReason;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    ClinicalOrderStatus status;
    @Column(name = "ordered_at", nullable = false)
    Instant orderedAt;
    @Column(name = "completed_at")
    Instant completedAt;
    @Column(name = "created_at", nullable = false)
    Instant createdAt;
    @Column(name = "updated_at")
    Instant updatedAt;
}

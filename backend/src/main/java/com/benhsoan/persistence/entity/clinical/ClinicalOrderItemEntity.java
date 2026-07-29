package com.benhsoan.persistence.entity.clinical;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.clinical.enums.ClinicalOrderItemStatus;

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
@Table(name = "clinical_order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicalOrderItemEntity {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    UUID id;
    @Column(name = "clinical_order_id", nullable = false, columnDefinition = "BINARY(16)")
    UUID clinicalOrderId;
    @Column(name = "clinical_service_id", nullable = false, columnDefinition = "BINARY(16)")
    UUID clinicalServiceId;
    @Column(name = "service_code", nullable = false, length = 30)
    String serviceCode;
    @Column(name = "service_name", nullable = false, length = 150)
    String serviceName;
    @Column(columnDefinition = "TEXT")
    String instruction;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    ClinicalOrderItemStatus status;
    @Column(name = "created_at", nullable = false)
    Instant createdAt;
    @Column(name = "updated_at")
    Instant updatedAt;
}

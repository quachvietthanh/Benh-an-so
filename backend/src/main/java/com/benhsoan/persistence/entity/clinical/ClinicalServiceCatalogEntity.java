package com.benhsoan.persistence.entity.clinical;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.clinical.enums.ClinicalResultDataType;
import com.benhsoan.domain.clinical.enums.ClinicalServiceType;

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
@Table(name = "clinical_service_catalog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicalServiceCatalogEntity {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    UUID id;
    @Column(name = "service_catalog_id", nullable = false, columnDefinition = "BINARY(16)")
    UUID serviceCatalogId;
    @Column(name = "service_code", nullable = false, length = 30)
    String serviceCode;
    @Column(name = "service_name", nullable = false, length = 150)
    String serviceName;
    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 30)
    ClinicalServiceType serviceType;
    @Enumerated(EnumType.STRING)
    @Column(name = "result_data_type", nullable = false, length = 30)
    ClinicalResultDataType resultDataType;
    @Column(length = 50)
    String unit;
    @Column(name = "reference_range", length = 255)
    String referenceRange;
    @Column(columnDefinition = "TEXT")
    String description;
    @Column(nullable = false)
    boolean active;
    @Column(name = "created_at", nullable = false)
    Instant createdAt;
    @Column(name = "updated_at")
    Instant updatedAt;
}

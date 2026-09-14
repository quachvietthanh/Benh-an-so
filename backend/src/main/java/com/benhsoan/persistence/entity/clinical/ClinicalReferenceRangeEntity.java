package com.benhsoan.persistence.entity.clinical;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.patient.enums.Gender;

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
@Table(name = "clinical_reference_ranges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicalReferenceRangeEntity {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    UUID id;

    @Column(name = "clinical_service_id", nullable = false, columnDefinition = "BINARY(16)")
    UUID clinicalServiceId;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    Gender gender;

    @Column(name = "min_age")
    Integer minAge;

    @Column(name = "max_age")
    Integer maxAge;

    @Column(name = "lower_bound", precision = 18, scale = 4)
    BigDecimal lowerBound;

    @Column(name = "upper_bound", precision = 18, scale = 4)
    BigDecimal upperBound;

    @Column(nullable = false)
    boolean active;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at")
    Instant updatedAt;
}

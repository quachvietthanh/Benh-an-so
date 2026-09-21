package com.benhsoan.persistence.entity.contraindication;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.contraindication.enums.ContraindicationSeverity;
import com.benhsoan.domain.contraindication.enums.ContraindicationType;

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
@Table(name = "contraindication_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContraindicationRuleEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "medicine_id", columnDefinition = "BINARY(16)")
    private UUID medicineId;

    @Column(name = "active_ingredient", length = 150)
    private String activeIngredient;

    @Enumerated(EnumType.STRING)
    @Column(name = "contraindication_type", nullable = false, length = 30)
    private ContraindicationType type;

    @Column(name = "min_age_years")
    private Integer minAgeYears;

    @Column(name = "max_age_years")
    private Integer maxAgeYears;

    @Column(name = "diagnosis_catalog_id", columnDefinition = "BINARY(16)")
    private UUID diagnosisCatalogId;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 30)
    private ContraindicationSeverity severity;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "recommendation", length = 500)
    private String recommendation;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}

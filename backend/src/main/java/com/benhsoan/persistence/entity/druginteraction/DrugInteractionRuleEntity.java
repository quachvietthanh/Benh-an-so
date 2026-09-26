package com.benhsoan.persistence.entity.druginteraction;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.druginteraction.enums.InteractionSeverity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "drug_interaction_rules",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_drug_interaction_rules_ingredient_pair",
                columnNames = {"active_ingredient_a", "active_ingredient_b"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrugInteractionRuleEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "active_ingredient_a", nullable = false, length = 255)
    private String activeIngredientA;

    @Column(name = "active_ingredient_b", nullable = false, length = 255)
    private String activeIngredientB;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity_level", nullable = false, length = 30)
    private InteractionSeverity severity;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "clinical_recommendation", nullable = false, columnDefinition = "TEXT")
    private String clinicalRecommendation;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}

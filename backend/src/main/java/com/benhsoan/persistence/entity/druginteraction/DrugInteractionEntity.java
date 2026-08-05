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
        name = "drug_interactions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_drug_interactions_medicine_pair",
                columnNames = {"first_medicine_id", "second_medicine_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrugInteractionEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "first_medicine_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID firstMedicineId;

    @Column(name = "second_medicine_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID secondMedicineId;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 30)
    private InteractionSeverity severity;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "recommendation", nullable = false, columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}

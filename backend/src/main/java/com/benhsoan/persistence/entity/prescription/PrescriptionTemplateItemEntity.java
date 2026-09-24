package com.benhsoan.persistence.entity.prescription;

import java.util.UUID;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;

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
        name = "prescription_template_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_prescription_template_items_medicine",
                columnNames = {"template_id", "medicine_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionTemplateItemEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "template_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID templateId;

    @Column(name = "medicine_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID medicineId;

    @Column(name = "dosage", nullable = false, length = 100)
    private String dosage;

    @Column(name = "frequency", nullable = false)
    private Integer frequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "route", nullable = false, length = 30)
    private AdministrationRoute route;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}

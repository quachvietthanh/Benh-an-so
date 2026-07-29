package com.benhsoan.persistence.entity.medicalrecord;

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
@Table(name = "diagnosis_catalog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagnosisCatalogEntity {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    UUID id;
    @Column(nullable = false, length = 30)
    String code;
    @Column(nullable = false, length = 150)
    String name;
    @Column(columnDefinition = "TEXT")
    String description;
    @Column(nullable = false)
    boolean active;
    @Column(name = "created_at", nullable = false)
    Instant createdAt;
    @Column(name = "updated_at")
    Instant updatedAt;
}

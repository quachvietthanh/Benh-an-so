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

@Entity
@Table(name = "medical_record_templates")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalRecordTemplateEntity {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    @Column(name = "specialty_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID specialtyId;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(name = "name_key", nullable = false, length = 150)
    private String nameKey;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "is_default", nullable = false)
    private boolean defaultTemplate;
    @Column(name = "current_version_no", nullable = false)
    private int currentVersionNo;
    @Column(name = "created_by", nullable = false, columnDefinition = "BINARY(16)")
    private UUID createdBy;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_by", columnDefinition = "BINARY(16)")
    private UUID updatedBy;
    @Column(name = "updated_at")
    private Instant updatedAt;
}

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
@Table(name = "medical_record_template_versions")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalRecordTemplateVersionEntity {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    @Column(name = "template_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID templateId;
    @Column(name = "version_no", nullable = false)
    private int versionNo;
    @Column(name = "specialty_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID specialtyId;
    @Column(name = "template_name", nullable = false, length = 150)
    private String templateName;
    @Column(name = "change_note", length = 500)
    private String changeNote;
    @Column(name = "created_by", nullable = false, columnDefinition = "BINARY(16)")
    private UUID createdBy;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

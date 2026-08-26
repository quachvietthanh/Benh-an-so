package com.benhsoan.persistence.entity.medicalrecord;

import java.util.UUID;

import com.benhsoan.domain.medicalrecord.enums.MedicalRecordFieldCode;

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

@Entity
@Table(name = "medical_record_template_sections")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalRecordTemplateSectionEntity {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    @Column(name = "template_version_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID templateVersionId;
    @Enumerated(EnumType.STRING)
    @Column(name = "field_code", nullable = false, length = 50)
    private MedicalRecordFieldCode fieldCode;
    @Column(nullable = false, length = 150)
    private String label;
    @Column(nullable = false)
    private boolean required;
    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}

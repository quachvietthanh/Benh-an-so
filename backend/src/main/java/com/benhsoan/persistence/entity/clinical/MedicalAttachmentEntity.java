package com.benhsoan.persistence.entity.clinical;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.clinical.enums.MedicalAttachmentType;

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
@Table(name = "medical_attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalAttachmentEntity {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    UUID id;
    @Column(name = "visit_id", nullable = false, columnDefinition = "BINARY(16)")
    UUID visitId;
    @Column(name = "medical_record_id", columnDefinition = "BINARY(16)")
    UUID medicalRecordId;
    @Column(name = "clinical_result_id", columnDefinition = "BINARY(16)")
    UUID clinicalResultId;
    @Column(name = "file_name", nullable = false, length = 255)
    String fileName;
    @Column(name = "original_file_name", nullable = false, length = 255)
    String originalFileName;
    @Column(name = "storage_key", nullable = false, length = 500)
    String storageKey;
    @Column(name = "content_type", nullable = false, length = 100)
    String contentType;
    @Column(name = "file_size", nullable = false)
    long fileSize;
    @Column(length = 128)
    String checksum;
    @Enumerated(EnumType.STRING)
    @Column(name = "attachment_type", nullable = false, length = 30)
    MedicalAttachmentType attachmentType;
    @Column(name = "uploaded_by", nullable = false, columnDefinition = "BINARY(16)")
    UUID uploadedBy;
    @Column(name = "uploaded_at", nullable = false)
    Instant uploadedAt;
}

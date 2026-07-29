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
@Table(name = "medical_record_amendments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalRecordAmendmentEntity {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    UUID id;
    @Column(name = "medical_record_id", nullable = false, columnDefinition = "BINARY(16)")
    UUID medicalRecordId;
    @Column(nullable = false, columnDefinition = "TEXT")
    String content;
    @Column(nullable = false, columnDefinition = "TEXT")
    String reason;
    @Column(name = "amended_by", nullable = false, columnDefinition = "BINARY(16)")
    UUID amendedBy;
    @Column(name = "amended_at", nullable = false)
    Instant amendedAt;
}

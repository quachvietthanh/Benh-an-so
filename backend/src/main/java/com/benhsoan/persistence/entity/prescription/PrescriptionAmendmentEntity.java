package com.benhsoan.persistence.entity.prescription;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
@Table(name = "prescription_amendments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionAmendmentEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "prescription_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID prescriptionId;

    @Column(name = "change_reason", nullable = false, columnDefinition = "TEXT")
    private String changeReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_data", nullable = false, columnDefinition = "JSON")
    private String beforeData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_data", nullable = false, columnDefinition = "JSON")
    private String afterData;

    @Column(name = "amended_by", nullable = false, columnDefinition = "BINARY(16)")
    private UUID amendedBy;

    @Column(name = "amended_at", nullable = false)
    private Instant amendedAt;
}

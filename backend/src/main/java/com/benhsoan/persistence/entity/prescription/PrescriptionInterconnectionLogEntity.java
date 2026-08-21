package com.benhsoan.persistence.entity.prescription;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.benhsoan.domain.prescription.enums.PrescriptionInterconnectionAttemptType;
import com.benhsoan.domain.prescription.enums.PrescriptionInterconnectionOutcome;

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
@Table(name = "prescription_interconnection_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionInterconnectionLogEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "prescription_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID prescriptionId;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "attempt_type", nullable = false, length = 10)
    private PrescriptionInterconnectionAttemptType attemptType;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 10)
    private PrescriptionInterconnectionOutcome outcome;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_payload", nullable = false, columnDefinition = "JSON")
    private String requestPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_payload", columnDefinition = "JSON")
    private String responsePayload;

    @Column(name = "receipt_code", length = 50)
    private String receiptCode;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "attempted_by", nullable = false, columnDefinition = "BINARY(16)")
    private UUID attemptedBy;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;
}

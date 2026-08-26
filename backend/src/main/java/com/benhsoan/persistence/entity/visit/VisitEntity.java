package com.benhsoan.persistence.entity.visit;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;

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
@Table(name = "visits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitEntity {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    @Column(name = "visit_code", nullable = false, length = 30)
    private String visitCode;
    @Column(name = "patient_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID patientId;
    @Column(name = "doctor_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID doctorId;
    @Column(name = "appointment_id", columnDefinition = "BINARY(16)")
    private UUID appointmentId;
    @Column(name = "queue_item_id", columnDefinition = "BINARY(16)")
    private UUID queueItemId;
    @Column(name = "specialty_id", columnDefinition = "BINARY(16)")
    private UUID specialtyId;
    @Enumerated(EnumType.STRING)
    @Column(name = "visit_type", nullable = false, length = 30)
    private VisitType visitType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VisitStatus status;
    @Column(name = "visit_at", nullable = false)
    private Instant visitAt;
    @Column(name = "started_at")
    private Instant startedAt;
    @Column(name = "completed_at")
    private Instant completedAt;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;
    @Column(columnDefinition = "TEXT")
    private String note;
    @Column(name = "created_by", nullable = false, columnDefinition = "BINARY(16)")
    private UUID createdBy;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at")
    private Instant updatedAt;
}

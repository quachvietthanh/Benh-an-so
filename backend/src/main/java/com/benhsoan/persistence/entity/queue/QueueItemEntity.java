package com.benhsoan.persistence.entity.queue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.queue.enums.QueueItemSourceType;
import com.benhsoan.domain.queue.enums.QueueItemStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "queue_items")
@Getter
@Setter
public class QueueItemEntity {
    @Id @Column(columnDefinition = "BINARY(16)") private UUID id;
    @Column(name = "medical_queue_id") private UUID medicalQueueId;
    @Column(name = "patient_id") private UUID patientId;
    @Column(name = "appointment_id") private UUID appointmentId;
    @Column(name = "visit_id") private UUID visitId;
    @Enumerated(EnumType.STRING) @Column(name = "source_type") private QueueItemSourceType sourceType;
    @Enumerated(EnumType.STRING) private QueueItemStatus status;
    @Column(name = "queue_number") private int queueNumber;
    @Column(name = "queue_date") private LocalDate queueDate;
    @Column(name = "checked_in_at") private Instant checkedInAt;
    @Column(name = "called_at") private Instant calledAt;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "cancelled_at") private Instant cancelledAt;
    @Column(name = "cancel_reason") private String cancelReason;
    @Column(name = "skipped_at") private Instant skippedAt;
    @Column(name = "skip_reason") private String skipReason;
    @Column(name = "created_by") private UUID createdBy;
    @Column(name = "created_at") private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;
}

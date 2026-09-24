package com.benhsoan.persistence.entity.portal;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.portal.notification.PatientPortalNotificationType;

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
@Table(name = "patient_portal_notifications")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientPortalNotificationEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "patient_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID patientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private PatientPortalNotificationType type;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "appointment_id", columnDefinition = "BINARY(16)")
    private UUID appointmentId;

    @Column(name = "reschedule_log_id", columnDefinition = "BINARY(16)")
    private UUID rescheduleLogId;

    @Column(name = "clinical_result_id", columnDefinition = "BINARY(16)")
    private UUID clinicalResultId;
}

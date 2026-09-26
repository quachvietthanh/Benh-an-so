package com.benhsoan.persistence.entity.appointment;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.appointment.notification.enums.NotificationChannel;
import com.benhsoan.domain.appointment.notification.enums.NotificationStatus;
import com.benhsoan.domain.appointment.notification.enums.NotificationType;

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
@Table(name = "appointment_notification_logs")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentNotificationLogEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "appointment_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID appointmentId;

    @Column(name = "patient_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID patientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 30)
    private NotificationChannel channel;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private NotificationStatus status;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

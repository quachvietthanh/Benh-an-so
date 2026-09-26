package com.benhsoan.domain.appointment.notification;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.appointment.notification.enums.NotificationChannel;
import com.benhsoan.domain.appointment.notification.enums.NotificationStatus;
import com.benhsoan.domain.appointment.notification.enums.NotificationType;
import com.benhsoan.domain.shared.Guard.Guard;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppointmentNotificationLog {

    private UUID id;
    private UUID appointmentId;
    private UUID patientId;
    private NotificationType notificationType;
    private NotificationChannel channel;
    private String content;
    private NotificationStatus status;
    private Instant attemptedAt;
    private Instant sentAt;
    private String failureReason;
    private Instant createdAt;

    private AppointmentNotificationLog(UUID id, UUID appointmentId, UUID patientId,
            NotificationType notificationType, NotificationChannel channel, String content,
            NotificationStatus status, Instant attemptedAt, Instant sentAt,
            String failureReason, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.appointmentId = Objects.requireNonNull(appointmentId);
        this.patientId = Objects.requireNonNull(patientId);
        this.notificationType = Objects.requireNonNull(notificationType);
        this.channel = Objects.requireNonNull(channel);
        this.content = Guard.require(content, "Notification content");
        this.status = Objects.requireNonNull(status);
        this.attemptedAt = Objects.requireNonNull(attemptedAt);
        this.sentAt = sentAt;
        this.failureReason = failureReason;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static AppointmentNotificationLog sent(UUID appointmentId, UUID patientId,
            String content, Instant attemptedAt) {
        return new AppointmentNotificationLog(UUID.randomUUID(), appointmentId, patientId,
                NotificationType.APPOINTMENT_REMINDER, NotificationChannel.MOCK, content,
                NotificationStatus.SENT, attemptedAt, attemptedAt, null, attemptedAt);
    }

    public static AppointmentNotificationLog failed(UUID appointmentId, UUID patientId,
            String content, String failureReason, Instant attemptedAt) {
        return new AppointmentNotificationLog(UUID.randomUUID(), appointmentId, patientId,
                NotificationType.APPOINTMENT_REMINDER, NotificationChannel.MOCK, content,
                NotificationStatus.FAILED, attemptedAt, null,
                Guard.require(failureReason, "Failure reason"), attemptedAt);
    }

    public static AppointmentNotificationLog restore(UUID id, UUID appointmentId, UUID patientId,
            NotificationType notificationType, NotificationChannel channel, String content,
            NotificationStatus status, Instant attemptedAt, Instant sentAt,
            String failureReason, Instant createdAt) {
        return new AppointmentNotificationLog(id, appointmentId, patientId, notificationType,
                channel, content, status, attemptedAt, sentAt, failureReason, createdAt);
    }
}

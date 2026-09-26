package com.benhsoan.domain.portal.notification;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.shared.Guard.Guard;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A patient-portal notification (NCL-14-CN-008). Title and message are stored as
 * a snapshot at creation time so later source-object changes never mutate the
 * historical notification text. Ownership is enforced via {@code patientId}
 * (QTN-23).
 */
@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PatientPortalNotification {

    private UUID id;
    private UUID patientId;
    private PatientPortalNotificationType type;
    private String title;
    private String message;
    private Instant readAt;
    private Instant createdAt;
    private UUID appointmentId;
    private UUID rescheduleLogId;
    private UUID clinicalResultId;

    /**
     * NCL-14-CN-010 TC-03: for {@code GUARDIAN_LINK_REVIEW} notifications this holds the
     * dependent patient whose adulthood triggered the review. It is the idempotency key for
     * that category (one review notification per recipient per dependent) and null for every
     * other notification type.
     */
    private UUID guardianReviewDependentPatientId;

    private PatientPortalNotification(
            UUID id,
            UUID patientId,
            PatientPortalNotificationType type,
            String title,
            String message,
            Instant readAt,
            Instant createdAt,
            UUID appointmentId,
            UUID rescheduleLogId,
            UUID clinicalResultId,
            UUID guardianReviewDependentPatientId
    ) {
        this.id = Objects.requireNonNull(id);
        this.patientId = Objects.requireNonNull(patientId);
        this.type = Objects.requireNonNull(type);
        this.title = Guard.require(title, "Notification title");
        this.message = Guard.require(message, "Notification message");
        this.readAt = readAt;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.appointmentId = appointmentId;
        this.rescheduleLogId = rescheduleLogId;
        this.clinicalResultId = clinicalResultId;
        this.guardianReviewDependentPatientId = guardianReviewDependentPatientId;
    }

    public static PatientPortalNotification reminder(
            UUID patientId,
            String title,
            String message,
            UUID appointmentId,
            Instant createdAt
    ) {
        return new PatientPortalNotification(UUID.randomUUID(), patientId,
                PatientPortalNotificationType.APPOINTMENT_REMINDER, title, message,
                null, createdAt, appointmentId, null, null, null);
    }

    public static PatientPortalNotification changed(
            UUID patientId,
            String title,
            String message,
            UUID appointmentId,
            UUID rescheduleLogId,
            Instant createdAt
    ) {
        return new PatientPortalNotification(UUID.randomUUID(), patientId,
                PatientPortalNotificationType.APPOINTMENT_CHANGED, title, message,
                null, createdAt, appointmentId, rescheduleLogId, null, null);
    }

    public static PatientPortalNotification labResultAvailable(
            UUID patientId,
            String title,
            String message,
            UUID clinicalResultId,
            Instant createdAt
    ) {
        return new PatientPortalNotification(UUID.randomUUID(), patientId,
                PatientPortalNotificationType.LAB_RESULT_AVAILABLE, title, message,
                null, createdAt, null, null, clinicalResultId, null);
    }

    /**
     * NCL-14-CN-010 TC-03: guardian-link review reminder. {@code patientId} is the recipient
     * (the dependent's own portal profile, or the guardian's portal profile) and
     * {@code dependentPatientId} identifies the link being reviewed, which also acts as the
     * idempotency key so repeated sweeps never duplicate the reminder.
     */
    public static PatientPortalNotification guardianLinkReview(
            UUID patientId,
            String title,
            String message,
            UUID dependentPatientId,
            Instant createdAt
    ) {
        return new PatientPortalNotification(UUID.randomUUID(), patientId,
                PatientPortalNotificationType.GUARDIAN_LINK_REVIEW, title, message,
                null, createdAt, null, null, null, dependentPatientId);
    }

    public static PatientPortalNotification restore(
            UUID id,
            UUID patientId,
            PatientPortalNotificationType type,
            String title,
            String message,
            Instant readAt,
            Instant createdAt,
            UUID appointmentId,
            UUID rescheduleLogId,
            UUID clinicalResultId,
            UUID guardianReviewDependentPatientId
    ) {
        return new PatientPortalNotification(id, patientId, type, title, message,
                readAt, createdAt, appointmentId, rescheduleLogId, clinicalResultId,
                guardianReviewDependentPatientId);
    }

    public boolean isRead() {
        return readAt != null;
    }

    /** Idempotent: sets the read timestamp only the first time. */
    public void markRead(Instant at) {
        if (readAt == null) {
            this.readAt = Objects.requireNonNull(at);
        }
    }
}

package com.benhsoan.domain.portal.notification;

import java.time.Instant;
import java.util.UUID;

/**
 * NCL-14-CN-008: domain event emitted after a clinical result finalization has
 * committed, requesting the creation of the matching patient-portal
 * notification. Delivered after commit so the notification's
 * {@code clinical_result_id} foreign key always references a committed row.
 */
public record LabResultAvailableNotificationRequested(
        UUID patientId,
        UUID clinicalResultId,
        Instant now
) {
}

package com.benhsoan.port.outbound.repository.portal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.portal.notification.PatientPortalNotification;
import com.benhsoan.domain.portal.notification.PatientPortalNotificationType;

public interface PatientPortalNotificationRepository {

    PatientPortalNotification save(PatientPortalNotification notification);

    List<PatientPortalNotification> findByPatientIdOrderByCreatedAtDesc(UUID patientId, int limit);

    Optional<PatientPortalNotification> findById(UUID id);

    boolean existsByPatientIdAndTypeAndAppointmentId(
            UUID patientId, PatientPortalNotificationType type, UUID appointmentId);

    boolean existsByPatientIdAndTypeAndRescheduleLogId(
            UUID patientId, PatientPortalNotificationType type, UUID rescheduleLogId);

    boolean existsByPatientIdAndTypeAndClinicalResultId(
            UUID patientId, PatientPortalNotificationType type, UUID clinicalResultId);

    boolean existsByPatientIdAndTypeAndGuardianReviewDependentPatientId(
            UUID patientId, PatientPortalNotificationType type, UUID guardianReviewDependentPatientId);
}

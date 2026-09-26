package com.benhsoan.persistence.jpaRepository.portal;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.domain.portal.notification.PatientPortalNotificationType;
import com.benhsoan.persistence.entity.portal.PatientPortalNotificationEntity;

public interface JpaPatientPortalNotificationRepository
        extends JpaRepository<PatientPortalNotificationEntity, UUID> {

    List<PatientPortalNotificationEntity> findByPatientIdOrderByCreatedAtDesc(
            UUID patientId, Pageable pageable);

    boolean existsByPatientIdAndTypeAndAppointmentId(
            UUID patientId, PatientPortalNotificationType type, UUID appointmentId);

    boolean existsByPatientIdAndTypeAndRescheduleLogId(
            UUID patientId, PatientPortalNotificationType type, UUID rescheduleLogId);

    boolean existsByPatientIdAndTypeAndClinicalResultId(
            UUID patientId, PatientPortalNotificationType type, UUID clinicalResultId);

    boolean existsByPatientIdAndTypeAndGuardianReviewDependentPatientId(
            UUID patientId, PatientPortalNotificationType type, UUID guardianReviewDependentPatientId);
}

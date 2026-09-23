package com.benhsoan.persistence.adapterRepository.portal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.benhsoan.domain.portal.notification.PatientPortalNotification;
import com.benhsoan.domain.portal.notification.PatientPortalNotificationType;
import com.benhsoan.persistence.jpaRepository.portal.JpaPatientPortalNotificationRepository;
import com.benhsoan.persistence.mapper.portal.PatientPortalNotificationPersistenceMapper;
import com.benhsoan.port.outbound.repository.portal.PatientPortalNotificationRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PatientPortalNotificationRepositoryAdapter
        implements PatientPortalNotificationRepository {

    private final JpaPatientPortalNotificationRepository jpaRepository;
    private final PatientPortalNotificationPersistenceMapper mapper;

    @Override
    public PatientPortalNotification save(PatientPortalNotification notification) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(notification)));
    }

    @Override
    public List<PatientPortalNotification> findByPatientIdOrderByCreatedAtDesc(UUID patientId, int limit) {
        return jpaRepository.findByPatientIdOrderByCreatedAtDesc(patientId, PageRequest.of(0, limit))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<PatientPortalNotification> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByPatientIdAndTypeAndAppointmentId(
            UUID patientId, PatientPortalNotificationType type, UUID appointmentId) {
        return jpaRepository.existsByPatientIdAndTypeAndAppointmentId(patientId, type, appointmentId);
    }

    @Override
    public boolean existsByPatientIdAndTypeAndRescheduleLogId(
            UUID patientId, PatientPortalNotificationType type, UUID rescheduleLogId) {
        return jpaRepository.existsByPatientIdAndTypeAndRescheduleLogId(patientId, type, rescheduleLogId);
    }

    @Override
    public boolean existsByPatientIdAndTypeAndClinicalResultId(
            UUID patientId, PatientPortalNotificationType type, UUID clinicalResultId) {
        return jpaRepository.existsByPatientIdAndTypeAndClinicalResultId(patientId, type, clinicalResultId);
    }
}

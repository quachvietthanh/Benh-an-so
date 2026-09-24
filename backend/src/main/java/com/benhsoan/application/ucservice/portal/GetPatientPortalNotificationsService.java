package com.benhsoan.application.ucservice.portal;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.patient.Patient;
import com.benhsoan.port.dto.result.portal.PatientPortalNotificationResult;
import com.benhsoan.port.inbound.portal.GetPatientPortalNotificationsUseCase;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.portal.PatientPortalNotificationRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

/**
 * NCL-14-CN-008: lists the authenticated patient's own notifications, newest
 * first. Ownership is derived from the security context (QTN-23), never from
 * client-supplied identifiers.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPatientPortalNotificationsService implements GetPatientPortalNotificationsUseCase {

    private final PatientPortalNotificationRepository notificationRepository;
    private final PatientRepository patientRepository;
    private final CurrentUserPort currentUserPort;
    private final PatientPortalNotificationResultMapper resultMapper;

    @Override
    public List<PatientPortalNotificationResult> getNotifications(int limit) {
        Patient patient = patientRepository.findByUserId(currentUserPort.getCurrentUserId())
                .orElseThrow(() -> new AccessDeniedException(
                        "No patient profile is linked to the authenticated user."));

        return notificationRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId(), limit)
                .stream()
                .map(resultMapper::toResult)
                .toList();
    }
}

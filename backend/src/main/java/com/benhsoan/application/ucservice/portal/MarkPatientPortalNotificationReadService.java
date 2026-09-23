package com.benhsoan.application.ucservice.portal;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.patient.PatientAccessGuard;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.portal.exception.PatientPortalNotificationNotFoundException;
import com.benhsoan.domain.portal.notification.PatientPortalNotification;
import com.benhsoan.port.dto.result.portal.PatientPortalNotificationResult;
import com.benhsoan.port.inbound.portal.MarkPatientPortalNotificationReadUseCase;
import com.benhsoan.port.outbound.repository.portal.PatientPortalNotificationRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

/**
 * NCL-14-CN-008: marks one notification as read after verifying ownership
 * (QTN-23). Idempotent: {@code readAt} is only set the first time.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MarkPatientPortalNotificationReadService implements MarkPatientPortalNotificationReadUseCase {

    private final PatientPortalNotificationRepository notificationRepository;
    private final PatientAccessGuard patientAccessGuard;
    private final PatientPortalNotificationResultMapper resultMapper;
    private final ClockPort clockPort;

    @Override
    public PatientPortalNotificationResult markRead(UUID id) {
        PatientPortalNotification notification = notificationRepository.findById(id)
                .orElseThrow(PatientPortalNotificationNotFoundException::new);

        patientAccessGuard.requirePatientOwnership(
                notification.getPatientId(), ResourceType.PATIENT_PORTAL, notification.getId());

        notification.markRead(clockPort.now());
        return resultMapper.toResult(notificationRepository.save(notification));
    }
}

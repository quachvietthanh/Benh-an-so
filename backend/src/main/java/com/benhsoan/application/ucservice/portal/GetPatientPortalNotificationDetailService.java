package com.benhsoan.application.ucservice.portal;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.patient.PatientAccessGuard;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.portal.exception.PatientPortalNotificationNotFoundException;
import com.benhsoan.domain.portal.notification.PatientPortalNotification;
import com.benhsoan.port.dto.result.portal.PatientPortalNotificationResult;
import com.benhsoan.port.inbound.portal.GetPatientPortalNotificationDetailUseCase;
import com.benhsoan.port.outbound.repository.portal.PatientPortalNotificationRepository;

import lombok.RequiredArgsConstructor;

/**
 * NCL-14-CN-008: retrieves a single notification after verifying patient
 * ownership (QTN-23). A missing notification and a foreign notification are both
 * mapped to a generic not-found/forbidden contract so existence is not leaked.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPatientPortalNotificationDetailService implements GetPatientPortalNotificationDetailUseCase {

    private final PatientPortalNotificationRepository notificationRepository;
    private final PatientAccessGuard patientAccessGuard;
    private final PatientPortalNotificationResultMapper resultMapper;

    @Override
    public PatientPortalNotificationResult getNotification(UUID id) {
        PatientPortalNotification notification = notificationRepository.findById(id)
                .orElseThrow(PatientPortalNotificationNotFoundException::new);

        patientAccessGuard.requirePatientOwnership(
                notification.getPatientId(), ResourceType.PATIENT_PORTAL, notification.getId());

        return resultMapper.toResult(notification);
    }
}

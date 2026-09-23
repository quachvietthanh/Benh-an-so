package com.benhsoan.application.ucservice.portal;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.patient.PatientAccessGuard;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.portal.exception.PatientPortalNotificationNotFoundException;
import com.benhsoan.domain.portal.notification.PatientPortalNotification;
import com.benhsoan.port.dto.result.portal.PatientPortalNotificationResult;
import com.benhsoan.port.inbound.portal.GetPatientPortalNotificationDetailUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.portal.PatientPortalNotificationRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * NCL-14-CN-008: retrieves a single notification after verifying patient
 * ownership (QTN-23). A missing notification yields 404, while an existing
 * notification owned by another patient is rejected by {@link PatientAccessGuard}
 * as 403 (IDOR) with an ACCESS_DENIED audit. Successful reads are audited as
 * {@code ActionType.READ} on {@code ResourceType.PATIENT_PORTAL}.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GetPatientPortalNotificationDetailService implements GetPatientPortalNotificationDetailUseCase {

    private static final String ONLINE_PORTAL = "ONLINE_PORTAL";

    private final PatientPortalNotificationRepository notificationRepository;
    private final PatientAccessGuard patientAccessGuard;
    private final PatientPortalNotificationResultMapper resultMapper;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final ObjectMapper objectMapper;

    @Override
    public PatientPortalNotificationResult getNotification(UUID id) {
        PatientPortalNotification notification = notificationRepository.findById(id)
                .orElseThrow(PatientPortalNotificationNotFoundException::new);

        patientAccessGuard.requirePatientOwnership(
                notification.getPatientId(), ResourceType.PATIENT_PORTAL, notification.getId());

        Instant viewedAt = clockPort.now();
        auditLogRepository.save(AuditLog.create(
                currentUserPort.getCurrentUserId(),
                ActionType.READ,
                ResourceType.PATIENT_PORTAL,
                notification.getId(),
                auditDetail(notification, viewedAt),
                null,
                viewedAt));

        return resultMapper.toResult(notification);
    }

    private String auditDetail(PatientPortalNotification notification, Instant viewedAt) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("channel", ONLINE_PORTAL);
        detail.put("notificationId", notification.getId().toString());
        detail.put("patientId", notification.getPatientId().toString());
        detail.put("viewedAt", viewedAt.toString());

        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize patient portal notification detail audit.", exception);
        }
    }
}

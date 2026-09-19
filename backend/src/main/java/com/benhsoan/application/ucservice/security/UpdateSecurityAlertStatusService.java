package com.benhsoan.application.ucservice.security;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.auditlog.AdminOperationAuditService;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.security.SecurityAlert;
import com.benhsoan.domain.security.enums.AlertStatus;
import com.benhsoan.domain.security.exception.SecurityAlertNotFoundException;
import com.benhsoan.port.dto.result.security.SecurityAlertResult;
import com.benhsoan.port.inbound.security.UpdateSecurityAlertStatusUseCase;
import com.benhsoan.port.outbound.repository.security.SecurityAlertRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateSecurityAlertStatusService implements UpdateSecurityAlertStatusUseCase {

    private final SecurityAlertRepository securityAlertRepository;
    private final ClockPort clockPort;
    private final AdminOperationAuditService adminOperationAuditService;
    private final CurrentUserPort currentUserPort;

    @Override
    public SecurityAlertResult updateStatus(UUID id, AlertStatus status) {
        SecurityAlert alert = securityAlertRepository.findById(id)
                .orElseThrow(() -> new SecurityAlertNotFoundException(id));

        AlertStatus beforeStatus = alert.getStatus();
        Instant now = clockPort.now();
        alert.updateStatus(status, now);
        SecurityAlert saved = securityAlertRepository.save(alert);

        adminOperationAuditService.record(
                currentUserPort.getCurrentUserId(),
                ActionType.UPDATE,
                ResourceType.SECURITY_ALERT,
                saved.getId(),
                AdminOperationAuditService.fields(
                        "alertType", saved.getAlertType() != null ? saved.getAlertType().name() : null,
                        "severity", saved.getSeverity() != null ? saved.getSeverity().name() : null,
                        "status", beforeStatus != null ? beforeStatus.name() : null
                ),
                AdminOperationAuditService.fields(
                        "alertType", saved.getAlertType() != null ? saved.getAlertType().name() : null,
                        "severity", saved.getSeverity() != null ? saved.getSeverity().name() : null,
                        "status", saved.getStatus() != null ? saved.getStatus().name() : null
                ),
                now
        );

        return toResult(saved);
    }

    private SecurityAlertResult toResult(SecurityAlert alert) {
        return new SecurityAlertResult(
                alert.getId(),
                alert.getUserId(),
                null,
                null,
                alert.getAlertType(),
                alert.getSeverity(),
                alert.getDescription(),
                alert.getAccessCount(),
                alert.getWindowStart(),
                alert.getWindowEnd(),
                alert.getStatus(),
                alert.getCreatedAt());
    }
}

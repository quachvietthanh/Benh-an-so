package com.benhsoan.application.ucservice.security;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.security.SecurityAlert;
import com.benhsoan.domain.security.enums.AlertStatus;
import com.benhsoan.domain.security.exception.SecurityAlertNotFoundException;
import com.benhsoan.port.dto.result.security.SecurityAlertResult;
import com.benhsoan.port.inbound.security.UpdateSecurityAlertStatusUseCase;
import com.benhsoan.port.outbound.repository.security.SecurityAlertRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateSecurityAlertStatusService implements UpdateSecurityAlertStatusUseCase {

    private final SecurityAlertRepository securityAlertRepository;
    private final ClockPort clockPort;

    @Override
    public SecurityAlertResult updateStatus(UUID id, AlertStatus status) {
        SecurityAlert alert = securityAlertRepository.findById(id)
                .orElseThrow(() -> new SecurityAlertNotFoundException(id));

        alert.updateStatus(status, clockPort.now());

        return toResult(securityAlertRepository.save(alert));
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

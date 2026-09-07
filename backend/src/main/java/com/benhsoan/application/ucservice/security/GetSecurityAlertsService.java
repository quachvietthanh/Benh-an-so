package com.benhsoan.application.ucservice.security;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.security.SecurityAlert;
import com.benhsoan.port.dto.result.security.SecurityAlertResult;
import com.benhsoan.port.inbound.security.GetSecurityAlertsUseCase;
import com.benhsoan.port.outbound.repository.security.SecurityAlertRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetSecurityAlertsService implements GetSecurityAlertsUseCase {

    private final SecurityAlertRepository securityAlertRepository;

    @Override
    public List<SecurityAlertResult> getSecurityAlerts() {
        return securityAlertRepository.findAll().stream()
                .map(this::toResult)
                .toList();
    }

    private SecurityAlertResult toResult(SecurityAlert alert) {
        return new SecurityAlertResult(
                alert.getId(),
                alert.getUserId(),
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

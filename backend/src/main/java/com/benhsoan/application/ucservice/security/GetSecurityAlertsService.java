package com.benhsoan.application.ucservice.security;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.security.SecurityAlert;
import com.benhsoan.port.dto.result.security.SecurityAlertResult;
import com.benhsoan.port.inbound.security.GetSecurityAlertsUseCase;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.security.SecurityAlertRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetSecurityAlertsService implements GetSecurityAlertsUseCase {

    private final SecurityAlertRepository securityAlertRepository;
    private final UserRepository userRepository;

    @Override
    public Page<SecurityAlertResult> getSecurityAlerts(Pageable pageable) {
        Page<SecurityAlert> alerts = securityAlertRepository.findAll(pageable);

        Map<UUID, User> usersById = userRepository.findAllById(
                        alerts.getContent().stream()
                                .map(SecurityAlert::getUserId)
                                .distinct()
                                .toList())
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        return alerts.map(alert -> toResult(alert, usersById.get(alert.getUserId())));
    }

    private SecurityAlertResult toResult(SecurityAlert alert, User user) {
        return new SecurityAlertResult(
                alert.getId(),
                alert.getUserId(),
                user == null ? null : user.getUsername(),
                user == null ? null : user.getFullName(),
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

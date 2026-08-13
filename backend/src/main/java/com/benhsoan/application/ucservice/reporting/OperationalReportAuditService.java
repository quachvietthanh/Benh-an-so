package com.benhsoan.application.ucservice.reporting;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OperationalReportAuditService {

    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;

    public void logExport(LocalDate from, LocalDate to) {
        UUID actorId = currentUserPort.getCurrentUserId();
        Instant exportedAt = clockPort.now();

        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.EXPORT,
                ResourceType.OPERATIONAL_REPORT,
                null,
                """
                {
                "reportType":"OPERATIONAL_REPORT",
                "role":"%s",
                "from":"%s",
                "to":"%s",
                "exportedAt":"%s"
                }
                """.formatted(
                        resolvePrimaryRole(currentUserPort.getCurrentUserRoles()),
                        from,
                        to,
                        exportedAt
                ),
                null,
                exportedAt
        ));
    }

    private String resolvePrimaryRole(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "UNKNOWN";
        }
        if (roles.contains("MANAGER")) {
            return "MANAGER";
        }
        return roles.stream()
                .sorted()
                .findFirst()
                .orElse("UNKNOWN");
    }
}

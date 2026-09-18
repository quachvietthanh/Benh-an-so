package com.benhsoan.application.ucservice.reporting;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.reporting.enums.ReportType;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationalReportAuditService {

    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;

    public void logExport(ReportType reportType, LocalDate from, LocalDate to) {
        logExport(reportType, from, to, null);
    }

    public void logExport(ReportType reportType, LocalDate from, LocalDate to, UUID doctorId) {
        UUID actorId = currentUserPort.getCurrentUserId();
        Instant exportedAt = clockPort.now();

        String detailJson = doctorId != null ? """
                {
                "reportType":"%s",
                "role":"%s",
                "from":"%s",
                "to":"%s",
                "doctorId":"%s",
                "exportedAt":"%s"
                }
                """.formatted(
                        reportType.name(),
                        resolvePrimaryRole(currentUserPort.getCurrentUserRoles()),
                        from,
                        to,
                        doctorId,
                        exportedAt
                ) : """
                {
                "reportType":"%s",
                "role":"%s",
                "from":"%s",
                "to":"%s",
                "exportedAt":"%s"
                }
                """.formatted(
                        reportType.name(),
                        resolvePrimaryRole(currentUserPort.getCurrentUserRoles()),
                        from,
                        to,
                        exportedAt
                );

        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.EXPORT,
                ResourceType.OPERATIONAL_REPORT,
                null,
                detailJson,
                null,
                exportedAt
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAccessDenied(ReportType reportType, String reason) {
        try {
            UUID actorId = currentUserPort.getCurrentUserId();
            Instant deniedAt = clockPort.now();
            String detailJson = """
                    {
                    "reportType":"%s",
                    "role":"%s",
                    "reason":"%s",
                    "deniedAt":"%s"
                    }
                    """.formatted(
                            reportType.name(),
                            resolvePrimaryRole(currentUserPort.getCurrentUserRoles()),
                            reason,
                            deniedAt
                    );

            auditLogRepository.save(AuditLog.create(
                    actorId,
                    ActionType.ACCESS_DENIED,
                    ResourceType.OPERATIONAL_REPORT,
                    null,
                    detailJson,
                    null,
                    deniedAt
            ));
        } catch (RuntimeException exception) {
            log.warn("Failed to record access denied audit log for report {}: {}",
                    reportType, exception.getMessage());
        }
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

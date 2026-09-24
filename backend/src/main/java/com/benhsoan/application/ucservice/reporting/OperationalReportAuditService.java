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
        logExport(reportType, from, to, null, false, null);
    }

    public void logExport(ReportType reportType, LocalDate from, LocalDate to, UUID doctorId) {
        logExport(reportType, from, to, doctorId, false, null);
    }

    public void logExport(
            ReportType reportType,
            LocalDate from,
            LocalDate to,
            UUID doctorId,
            boolean unmasked,
            String unmaskReason
    ) {
        UUID actorId = currentUserPort.getCurrentUserId();
        Instant exportedAt = clockPort.now();

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("\"reportType\":\"").append(reportType.name()).append("\",\n");
        sb.append("\"role\":\"").append(resolvePrimaryRole(currentUserPort.getCurrentUserRoles())).append("\",\n");
        sb.append("\"from\":\"").append(from).append("\",\n");
        sb.append("\"to\":\"").append(to).append("\",\n");
        if (doctorId != null) {
            sb.append("\"doctorId\":\"").append(doctorId).append("\",\n");
        }
        sb.append("\"unmasked\":").append(unmasked).append(",\n");
        if (unmasked && unmaskReason != null) {
            sb.append("\"unmaskReason\":\"").append(escapeJson(unmaskReason)).append("\",\n");
        }
        sb.append("\"exportedAt\":\"").append(exportedAt).append("\"\n");
        sb.append("}");

        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.EXPORT,
                ResourceType.OPERATIONAL_REPORT,
                null,
                sb.toString(),
                null,
                exportedAt
        ));
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
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

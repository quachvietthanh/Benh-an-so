package com.benhsoan.application.ucservice.reporting;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.reporting.exception.AccessLogReportDataEmptyException;
import com.benhsoan.port.dto.result.AccessLogAccountCountResult;
import com.benhsoan.port.dto.result.AccessLogReportExportResult;
import com.benhsoan.port.dto.result.AccessLogReportItemResult;
import com.benhsoan.port.inbound.reporting.ExportAccessLogReportUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordAccessLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ExportAccessLogReportService implements ExportAccessLogReportUseCase {

    private static final String CSV_CONTENT_TYPE = "text/csv; charset=UTF-8";
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final MedicalRecordAccessLogRepository accessLogRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final ObjectMapper objectMapper;

    @Override
    public AccessLogReportExportResult export(LocalDate from, LocalDate to) {
        Instant fromInstant = from.atStartOfDay(CLINIC_ZONE).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(CLINIC_ZONE).toInstant();

        List<AccessLogAccountCountResult> counts = accessLogRepository.countAccessByAccountBetween(fromInstant, toInstant);
        if (counts.isEmpty()) {
            throw new AccessLogReportDataEmptyException();
        }

        Map<UUID, User> usersById = resolveUsers(counts);
        List<AccessLogReportItemResult> items = counts.stream()
                .map(count -> toItem(count, usersById.get(count.accessedBy())))
                .toList();

        Instant exportedAt = clockPort.now();
        byte[] content = buildCsv(from, to, items, exportedAt).getBytes(StandardCharsets.UTF_8);

        auditExport(from, to, exportedAt);

        return new AccessLogReportExportResult(buildFileName(from, to), CSV_CONTENT_TYPE, content);
    }

    private Map<UUID, User> resolveUsers(List<AccessLogAccountCountResult> counts) {
        List<UUID> ids = counts.stream()
                .map(AccessLogAccountCountResult::accessedBy)
                .distinct()
                .toList();
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
    }

    private AccessLogReportItemResult toItem(AccessLogAccountCountResult count, User user) {
        String username = user != null ? user.getUsername() : count.accessedBy().toString();
        String fullName = user != null ? user.getFullName() : "";
        return new AccessLogReportItemResult(username, fullName, count.accessCount());
    }

    private String buildFileName(LocalDate from, LocalDate to) {
        return "access-log-report-" + from + "-to-" + to + ".csv";
    }

    private String buildCsv(LocalDate from, LocalDate to, List<AccessLogReportItemResult> items, Instant exportedAt) {
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF'); // UTF-8 BOM so Excel opens the CSV correctly.
        csv.append("MEDICAL RECORD ACCESS LOG REPORT\n");
        csv.append("From,").append(from).append('\n');
        csv.append("To,").append(to).append('\n');
        csv.append("Generated At,").append(exportedAt).append("\n\n");
        csv.append("Account,Full Name,Access Count\n");

        for (AccessLogReportItemResult item : items) {
            csv.append(csvCell(item.username())).append(',')
                    .append(csvCell(item.fullName())).append(',')
                    .append(item.accessCount()).append('\n');
        }

        return csv.toString();
    }

    private String csvCell(String value) {
        if (value == null) {
            return "";
        }
        String neutralized = neutralizeFormulaInjection(value);
        if (neutralized.contains(",") || neutralized.contains("\"") || neutralized.contains("\n")) {
            return "\"" + neutralized.replace("\"", "\"\"") + "\"";
        }
        return neutralized;
    }

    /**
     * Neutralize spreadsheet formula injection (CSV injection, OWASP) for
     * account-controlled text before it is written into a CSV cell.
     * Leading {@code '} prevents spreadsheet applications from evaluating
     * the value as a formula/command.
     */
    private String neutralizeFormulaInjection(String value) {
        if (value.isEmpty()) {
            return value;
        }
        char first = value.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t') {
            return "'" + value;
        }
        return value;
    }

    private void auditExport(LocalDate from, LocalDate to, Instant exportedAt) {
        UUID actorId = currentUserPort.getCurrentUserId();

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("reportType", "ACCESS_LOG_REPORT");
        detail.put("from", from.toString());
        detail.put("to", to.toString());
        detail.put("exportedAt", exportedAt.toString());

        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.EXPORT,
                ResourceType.ACCESS_LOG_REPORT,
                null,
                toJson(detail),
                null,
                exportedAt));
    }

    private String toJson(Map<String, Object> detail) {
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException e) {
            return "{\"reportType\":\"ACCESS_LOG_REPORT\"}";
        }
    }
}

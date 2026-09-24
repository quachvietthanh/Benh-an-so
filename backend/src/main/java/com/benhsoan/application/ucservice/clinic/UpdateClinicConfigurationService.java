package com.benhsoan.application.ucservice.clinic;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.clinic.ClinicConfiguration;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.clinic.UpdateClinicConfigurationCommand;
import com.benhsoan.port.dto.result.clinic.ClinicConfigurationResult;
import com.benhsoan.port.inbound.clinic.UpdateClinicConfigurationUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateClinicConfigurationService implements UpdateClinicConfigurationUseCase {

    private final ClinicConfigurationRepository clinicConfigurationRepository;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final ClinicConfigurationResultMapper resultMapper;

    @Override
    public ClinicConfigurationResult update(UpdateClinicConfigurationCommand command) {
        if (command == null) {
            throw new ValidationException("Update clinic configuration command is required.");
        }

        Instant now = clockPort.now();
        ClinicConfiguration existing = clinicConfigurationRepository.find().orElse(null);

        int beforeRetentionYears = existing != null ? existing.getRetentionYears() : ClinicConfiguration.DEFAULT_RETENTION_YEARS;
        int afterRetentionYears = command.retentionYears() != null
                ? command.retentionYears()
                : beforeRetentionYears;

        int beforeSigningDeadlineHours = existing != null ? existing.getSigningDeadlineHours() : ClinicConfiguration.DEFAULT_SIGNING_DEADLINE_HOURS;
        int afterSigningDeadlineHours = command.signingDeadlineHours() != null
                ? command.signingDeadlineHours()
                : beforeSigningDeadlineHours;

        int beforeSessionTimeoutMinutes = existing != null ? existing.getSessionTimeoutMinutes() : ClinicConfiguration.DEFAULT_SESSION_TIMEOUT_MINUTES;
        int afterSessionTimeoutMinutes = command.sessionTimeoutMinutes() != null
                ? command.sessionTimeoutMinutes()
                : beforeSessionTimeoutMinutes;

        int beforeSessionWarningMinutes = existing != null ? existing.getSessionWarningMinutes() : ClinicConfiguration.DEFAULT_SESSION_WARNING_MINUTES;
        int afterSessionWarningMinutes = command.sessionWarningMinutes() != null
                ? command.sessionWarningMinutes()
                : beforeSessionWarningMinutes;

        ClinicConfiguration configuration = existing != null
                ? update(existing, command, afterRetentionYears, afterSigningDeadlineHours,
                        afterSessionTimeoutMinutes, afterSessionWarningMinutes, now)
                : ClinicConfiguration.create(
                        command.clinicName(),
                        command.address(),
                        command.phone(),
                        command.openingTime(),
                        command.closingTime(),
                        afterRetentionYears,
                        afterSigningDeadlineHours,
                        afterSessionTimeoutMinutes,
                        afterSessionWarningMinutes,
                        now
                );

        ClinicConfiguration saved = clinicConfigurationRepository.save(configuration);
        auditConfigurationUpdate(beforeRetentionYears, saved.getRetentionYears(),
                beforeSigningDeadlineHours, saved.getSigningDeadlineHours(),
                beforeSessionTimeoutMinutes, saved.getSessionTimeoutMinutes(),
                beforeSessionWarningMinutes, saved.getSessionWarningMinutes(), now);

        return resultMapper.toResult(saved);
    }

    private static ClinicConfiguration update(
            ClinicConfiguration configuration,
            UpdateClinicConfigurationCommand command,
            int retentionYears,
            int signingDeadlineHours,
            int sessionTimeoutMinutes,
            int sessionWarningMinutes,
            Instant updatedAt
    ) {
        configuration.update(
                command.clinicName(),
                command.address(),
                command.phone(),
                command.openingTime(),
                command.closingTime(),
                retentionYears,
                signingDeadlineHours,
                updatedAt
        );
        configuration.updateSessionSettings(sessionTimeoutMinutes, sessionWarningMinutes, updatedAt);
        return configuration;
    }

    private void auditConfigurationUpdate(
            int beforeRetentionYears, int afterRetentionYears,
            int beforeSigningDeadlineHours, int afterSigningDeadlineHours,
            int beforeSessionTimeoutMinutes, int afterSessionTimeoutMinutes,
            int beforeSessionWarningMinutes, int afterSessionWarningMinutes,
            Instant now
    ) {
        UUID actorId = currentUserPort.getCurrentUserId();
        String detail = """
                {"before":{"retentionYears":%d,"signingDeadlineHours":%d,"sessionTimeoutMinutes":%d,"sessionWarningMinutes":%d},"after":{"retentionYears":%d,"signingDeadlineHours":%d,"sessionTimeoutMinutes":%d,"sessionWarningMinutes":%d},"summary":"Clinic configuration updated"}
                """.formatted(
                beforeRetentionYears, beforeSigningDeadlineHours, beforeSessionTimeoutMinutes, beforeSessionWarningMinutes,
                afterRetentionYears, afterSigningDeadlineHours, afterSessionTimeoutMinutes, afterSessionWarningMinutes
        ).trim();

        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.UPDATE,
                ResourceType.CONFIGURATION,
                null,
                detail,
                null,
                now
        ));
    }
}

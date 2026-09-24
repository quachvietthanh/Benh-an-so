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
        int beforeRetentionYears = clinicConfigurationRepository.find()
                .map(ClinicConfiguration::getRetentionYears)
                .orElse(ClinicConfiguration.DEFAULT_RETENTION_YEARS);
        int afterRetentionYears = command.retentionYears() != null
                ? command.retentionYears()
                : beforeRetentionYears;

        int beforeSigningDeadlineHours = clinicConfigurationRepository.find()
                .map(ClinicConfiguration::getSigningDeadlineHours)
                .orElse(ClinicConfiguration.DEFAULT_SIGNING_DEADLINE_HOURS);
        int afterSigningDeadlineHours = command.signingDeadlineHours() != null
                ? command.signingDeadlineHours()
                : beforeSigningDeadlineHours;

        int beforeSessionIdleTimeoutMinutes = clinicConfigurationRepository.find()
                .map(ClinicConfiguration::getSessionIdleTimeoutMinutes)
                .orElse(ClinicConfiguration.DEFAULT_SESSION_IDLE_TIMEOUT_MINUTES);
        int afterSessionIdleTimeoutMinutes = command.sessionIdleTimeoutMinutes() != null
                ? command.sessionIdleTimeoutMinutes()
                : beforeSessionIdleTimeoutMinutes;

        ClinicConfiguration configuration = clinicConfigurationRepository.find()
                .map(existing -> update(existing, command, afterRetentionYears, afterSigningDeadlineHours, afterSessionIdleTimeoutMinutes, now))
                .orElseGet(() -> ClinicConfiguration.create(
                        command.clinicName(),
                        command.address(),
                        command.phone(),
                        command.openingTime(),
                        command.closingTime(),
                        afterRetentionYears,
                        afterSigningDeadlineHours,
                        afterSessionIdleTimeoutMinutes,
                        now
                ));

        ClinicConfiguration saved = clinicConfigurationRepository.save(configuration);
        auditConfigurationUpdate(beforeRetentionYears, saved.getRetentionYears(),
                beforeSigningDeadlineHours, saved.getSigningDeadlineHours(),
                beforeSessionIdleTimeoutMinutes, saved.getSessionIdleTimeoutMinutes(), now);

        return resultMapper.toResult(saved);
    }

    private static ClinicConfiguration update(
            ClinicConfiguration configuration,
            UpdateClinicConfigurationCommand command,
            int retentionYears,
            int signingDeadlineHours,
            int sessionIdleTimeoutMinutes,
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
                sessionIdleTimeoutMinutes,
                updatedAt
        );
        return configuration;
    }

    private void auditConfigurationUpdate(
            int beforeRetentionYears, int afterRetentionYears,
            int beforeSigningDeadlineHours, int afterSigningDeadlineHours,
            int beforeSessionIdleTimeoutMinutes, int afterSessionIdleTimeoutMinutes,
            Instant now
    ) {
        UUID actorId = currentUserPort.getCurrentUserId();
        String detail = """
                {"before":{"retentionYears":%d,"signingDeadlineHours":%d,"sessionIdleTimeoutMinutes":%d},"after":{"retentionYears":%d,"signingDeadlineHours":%d,"sessionIdleTimeoutMinutes":%d},"summary":"Clinic configuration updated; retentionYears changed from %d to %d; signingDeadlineHours changed from %d to %d; sessionIdleTimeoutMinutes changed from %d to %d"}
                """.formatted(
                beforeRetentionYears, beforeSigningDeadlineHours, beforeSessionIdleTimeoutMinutes,
                afterRetentionYears, afterSigningDeadlineHours, afterSessionIdleTimeoutMinutes,
                beforeRetentionYears, afterRetentionYears,
                beforeSigningDeadlineHours, afterSigningDeadlineHours,
                beforeSessionIdleTimeoutMinutes, afterSessionIdleTimeoutMinutes
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

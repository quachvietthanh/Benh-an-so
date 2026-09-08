package com.benhsoan.application.ucservice.anonymization;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.anonymization.AnonymizationMode;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.anonymization.UpdateAnonymizationModeCommand;
import com.benhsoan.port.dto.result.anonymization.AnonymizationModeResult;
import com.benhsoan.port.inbound.anonymization.GetAnonymizationModeUseCase;
import com.benhsoan.port.inbound.anonymization.UpdateAnonymizationModeUseCase;
import com.benhsoan.port.outbound.repository.anonymization.AnonymizationModeRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/**
 * Owns the anonymization mode (NCL-15-CN-003). The persistent
 * {@code system_configuration} table is the single source of truth; the
 * in-memory {@link AnonymizationModeState} acts as a cache that is loaded on
 * startup and updated after every change so the mode survives restarts and can
 * later be shared across instances.
 */
@Service
@RequiredArgsConstructor
public class PatientAnonymizationService
        implements GetAnonymizationModeUseCase, UpdateAnonymizationModeUseCase {

    private final AnonymizationModeRepository repository;
    private final AnonymizationModeState state;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;

    @PostConstruct
    void initialize() {
        repository.find().ifPresent(mode -> state.setEnabled(mode.enabled()));
    }

    public boolean isEnabled() {
        return state.isEnabled();
    }

    @Override
    @Transactional(readOnly = true)
    public AnonymizationModeResult get() {
        return repository.find()
                .map(mode -> new AnonymizationModeResult(mode.enabled(), mode.updatedAt()))
                .orElseGet(() -> new AnonymizationModeResult(false, null));
    }

    @Override
    @Transactional
    public AnonymizationModeResult update(UpdateAnonymizationModeCommand command) {
        if (command == null) {
            throw new ValidationException("Update anonymization mode command is required.");
        }
        Instant now = clockPort.now();
        UUID actorId = currentUserPort.getCurrentUserId();
        boolean before = state.isEnabled();
        boolean after = command.enabled();

        AnonymizationMode saved = repository.save(AnonymizationMode.of(after, now), actorId);
        state.setEnabled(after);
        audit(before, after, actorId, now);

        return new AnonymizationModeResult(saved.enabled(), saved.updatedAt());
    }

    private void audit(boolean before, boolean after, UUID actorId, Instant now) {
        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.UPDATE,
                ResourceType.CONFIGURATION,
                null,
                "Anonymization mode changed from " + before + " to " + after,
                null,
                now
        ));
    }
}

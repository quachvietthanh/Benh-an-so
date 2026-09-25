package com.benhsoan.application.ucservice.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auth.UserSession;
import com.benhsoan.domain.clinic.ClinicConfiguration;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.auth.ExtendSessionResult;
import com.benhsoan.port.inbound.auth.ExtendSessionUseCase;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ExtendSessionService implements ExtendSessionUseCase {

    private final UserSessionRepository userSessionRepository;
    private final ClinicConfigurationRepository clinicConfigurationRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;

    @Override
    public ExtendSessionResult extendCurrentSession() {
        UUID currentSessionId = currentUserPort.getCurrentSessionId();
        UserSession session = null;

        if (currentSessionId != null) {
            session = userSessionRepository.findById(currentSessionId).orElse(null);
        }

        if (session == null) {
            UUID currentUserId = currentUserPort.getCurrentUserId();
            session = userSessionRepository.findByUserId(currentUserId)
                    .orElseThrow(() -> new ValidationException("No active session found to extend."));
        }

        int timeoutMinutes = clinicConfigurationRepository.find()
                .map(ClinicConfiguration::getSessionIdleTimeoutMinutes)
                .orElse(ClinicConfiguration.DEFAULT_SESSION_IDLE_TIMEOUT_MINUTES);
        Duration idleTimeout = Duration.ofMinutes(timeoutMinutes);

        Instant now = clockPort.now();
        session.extend(now, idleTimeout);
        UserSession saved = userSessionRepository.save(session);

        return new ExtendSessionResult(
                saved.getId(),
                saved.getLastUsedAt(),
                saved.getLastUsedAt().plus(idleTimeout),
                "Session extended successfully"
        );
    }
}

package com.benhsoan.application.ucservice.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.auth.UserSession;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.auth.ExtendSessionResult;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExtendSessionService Tests")
class ExtendSessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-24T08:00:00Z");

    @Mock private UserSessionRepository userSessionRepository;
    @Mock private ClinicConfigurationRepository clinicConfigurationRepository;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private ClockPort clockPort;

    private ExtendSessionService service;

    @BeforeEach
    void setUp() {
        service = new ExtendSessionService(
                userSessionRepository,
                clinicConfigurationRepository,
                currentUserPort,
                clockPort
        );
    }

    @Test
    @DisplayName("Gia hạn phiên hiện tại thành công theo cấu hình phòng khám")
    void extendCurrentSessionSuccess() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        UserSession session = UserSession.restore(
                sessionId, userId, "hash", null,
                NOW.plus(Duration.ofDays(7)), NOW.minus(Duration.ofMinutes(25)), NOW.minus(Duration.ofMinutes(25)), null
        );

        when(currentUserPort.getCurrentSessionId()).thenReturn(sessionId);
        when(userSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(clinicConfigurationRepository.find()).thenReturn(Optional.empty()); // default 30 mins
        when(clockPort.now()).thenReturn(NOW);
        when(userSessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExtendSessionResult result = service.extendCurrentSession();

        assertNotNull(result);
        assertEquals(sessionId, result.sessionId());
        assertEquals(NOW, result.lastUsedAt());
        assertEquals(NOW.plus(Duration.ofMinutes(30)), result.idleExpiresAt());
        verify(userSessionRepository).save(session);
    }

    @Test
    @DisplayName("Gia hạn thất bại ném ValidationException khi phiên đã bị idle timeout")
    void extendCurrentSessionFailsWhenIdleTimeout() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        UserSession session = UserSession.restore(
                sessionId, userId, "hash", null,
                NOW.plus(Duration.ofDays(7)), NOW.minus(Duration.ofMinutes(40)), NOW.minus(Duration.ofMinutes(35)), null
        );

        when(currentUserPort.getCurrentSessionId()).thenReturn(sessionId);
        when(userSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(clinicConfigurationRepository.find()).thenReturn(Optional.empty());
        when(clockPort.now()).thenReturn(NOW);

        assertThrows(ValidationException.class, () -> service.extendCurrentSession());
    }
}

package com.benhsoan.application.ucservice.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auth.UserSession;
import com.benhsoan.port.dto.command.auth.LogoutCommand;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.crudRepository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.repository.logRepository.AuditLogRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class LogoutServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-28T04:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();

    @Mock private JwtTokenPort jwtTokenPort;
    @Mock private UserSessionRepository userSessionRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ClockPort clockPort;
    @Captor private ArgumentCaptor<UserSession> sessionCaptor;
    @Captor private ArgumentCaptor<AuditLog> auditCaptor;
    @InjectMocks private LogoutService service;

    @Test
    void revokesSessionBySessionIdAndWritesLogoutAuditLog() {
        UserSession session = UserSession.restore(SESSION_ID, USER_ID, "hash", null,
                NOW.plus(Duration.ofDays(7)), NOW, NOW, null);
        when(jwtTokenPort.validate("access-token")).thenReturn(true);
        when(jwtTokenPort.getSessionId("access-token")).thenReturn(SESSION_ID);
        when(userSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(clockPort.now()).thenReturn(NOW);
        when(userSessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.logout(new LogoutCommand("access-token"));

        verify(userSessionRepository).save(sessionCaptor.capture());
        verify(auditLogRepository).save(auditCaptor.capture());
        assertEquals(NOW, sessionCaptor.getValue().getRevokedAt());
        assertEquals(ActionType.LOGOUT, auditCaptor.getValue().getActionType());
        assertEquals(SESSION_ID, auditCaptor.getValue().getResourceId());
    }
}

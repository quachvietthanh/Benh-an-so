package com.benhsoan.application.ucservice.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.UserSession;
import com.benhsoan.domain.auth.exception.SessionExpiredException;
import com.benhsoan.domain.auth.exception.SessionNotFoundException;
import com.benhsoan.domain.session.SessionSettings;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class ExtendSessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-24T08:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID ROLE_ID = UUID.randomUUID();

    @Mock
    private UserSessionRepository userSessionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private CurrentUserPort currentUserPort;
    @Mock
    private SessionConfigurationProvider sessionConfigurationProvider;
    @Mock
    private ClockPort clockPort;
    @Mock
    private AuditLogRepository auditLogRepository;

    private ExtendSessionService service() {
        return new ExtendSessionService(userSessionRepository, userRepository, roleRepository,
                currentUserPort, sessionConfigurationProvider, new SessionResultMapper(),
                clockPort, auditLogRepository);
    }

    private void configureContextAndSettings() {
        when(currentUserPort.getCurrentSessionId()).thenReturn(SESSION_ID);
        when(currentUserPort.getCurrentUserId()).thenReturn(USER_ID);
        when(clockPort.now()).thenReturn(NOW);
        when(sessionConfigurationProvider.currentSettings())
                .thenReturn(new SessionSettings(Duration.ofMinutes(30), Duration.ofMinutes(5)));
    }

    @Test
    void extendsActiveSessionAndAudits() {
        configureContextAndSettings();
        UserSession session = UserSession.restore(SESSION_ID, USER_ID, "hash", null,
                NOW.plus(Duration.ofDays(7)), NOW.minusSeconds(60), NOW.minusSeconds(60), null);
        when(userSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(
                User.restore(USER_ID, "admin", "hash", "Admin", "a@b.c", null, ROLE_ID, true, null, NOW)));
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(
                Role.restore(ROLE_ID, "ADMIN", null, true, NOW, NOW, Set.of())));

        var result = service().extend();

        verify(userSessionRepository).touchLastUsed(SESSION_ID, NOW);
        verify(auditLogRepository).save(any());
        assertEquals(NOW, result.lastActivityAt());
    }

    @Test
    void rejectsRevokedSession() {
        configureContextAndSettings();
        UserSession session = UserSession.restore(SESSION_ID, USER_ID, "hash", null,
                NOW.plus(Duration.ofDays(7)), NOW, NOW, NOW);
        when(userSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        assertThrows(SessionExpiredException.class, () -> service().extend());

        verify(userSessionRepository, never()).touchLastUsed(eq(SESSION_ID), any());
    }

    @Test
    void rejectsIdleTimedOutSession() {
        configureContextAndSettings();
        UserSession session = UserSession.restore(SESSION_ID, USER_ID, "hash", null,
                NOW.plus(Duration.ofDays(7)), NOW, NOW.minus(Duration.ofMinutes(31)), null);
        when(userSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        assertThrows(SessionExpiredException.class, () -> service().extend());

        verify(userSessionRepository, never()).touchLastUsed(eq(SESSION_ID), any());
    }

    @Test
    void rejectsUnknownSession() {
        configureContextAndSettings();
        when(userSessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

        assertThrows(SessionNotFoundException.class, () -> service().extend());
    }
}
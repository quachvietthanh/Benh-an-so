package com.benhsoan.application.ucservice.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.benhsoan.domain.auth.exception.SessionNotFoundException;
import com.benhsoan.domain.session.SessionSettings;
import com.benhsoan.port.dto.result.session.SessionStatusResult;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

@ExtendWith(MockitoExtension.class)
class GetCurrentSessionServiceTest {

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

    private GetCurrentSessionService service() {
        return new GetCurrentSessionService(userSessionRepository, userRepository, roleRepository,
                currentUserPort, sessionConfigurationProvider, new SessionResultMapper());
    }

    @Test
    void returnsStatusForCurrentSession() {
        when(currentUserPort.getCurrentSessionId()).thenReturn(SESSION_ID);
        when(currentUserPort.getCurrentUserId()).thenReturn(USER_ID);
        when(sessionConfigurationProvider.currentSettings())
                .thenReturn(new SessionSettings(Duration.ofMinutes(30), Duration.ofMinutes(5)));

        UserSession session = UserSession.restore(SESSION_ID, USER_ID, "hash", null,
                NOW.plus(Duration.ofDays(7)), NOW, NOW, null);
        User user = User.restore(USER_ID, "admin", "hash", "Admin", "a@b.c", null, ROLE_ID, true, null, NOW);
        Role role = Role.restore(ROLE_ID, "ADMIN", null, true, NOW, NOW, Set.of());

        when(userSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role));

        SessionStatusResult result = service().getCurrentSession();

        assertEquals(SESSION_ID, result.sessionId());
        assertEquals("admin", result.username());
        assertEquals("Admin", result.fullName());
        assertEquals("ADMIN", result.role());
        assertEquals(NOW.plus(Duration.ofMinutes(30)), result.expiresAt());
        assertEquals(NOW.plus(Duration.ofMinutes(25)), result.warningAt());
        assertEquals(1800, result.inactivityTimeoutSeconds());
    }

    @Test
    void rejectsWhenSessionDoesNotBelongToUser() {
        when(currentUserPort.getCurrentSessionId()).thenReturn(SESSION_ID);
        when(currentUserPort.getCurrentUserId()).thenReturn(USER_ID);
        when(userSessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(UserSession.restore(SESSION_ID, UUID.randomUUID(), "hash", null,
                        NOW.plus(Duration.ofDays(7)), NOW, NOW, null)));

        assertThrows(SessionNotFoundException.class, () -> service().getCurrentSession());
    }

    @Test
    void rejectsWhenNoSessionInContext() {
        when(currentUserPort.getCurrentSessionId()).thenReturn(null);
        when(currentUserPort.getCurrentUserId()).thenReturn(USER_ID);

        assertThrows(SessionNotFoundException.class, () -> service().getCurrentSession());
    }
}
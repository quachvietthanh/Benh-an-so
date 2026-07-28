package com.benhsoan.infrastructure.authSecurity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.UserSession;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.crudRepository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.crudRepository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final Instant NOW = Instant.parse("2026-07-28T04:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();

    @Mock private JwtTokenPort jwtTokenPort;
    @Mock private UserSessionRepository userSessionRepository;
    @Mock private UserRepository userRepository;
    @Mock private ClockPort clockPort;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesOnlyActiveUserWithActiveSession() throws Exception {
        UserSession session = UserSession.restore(SESSION_ID, USER_ID, "hash", null,
                NOW.plus(Duration.ofDays(7)), NOW, NOW, null);
        User user = activeUser();
        JwtAuthenticationFilter filter = configuredFilter(session, user);
        when(jwtTokenPort.getUsername("access-token")).thenReturn("admin");
        when(jwtTokenPort.getRole("access-token")).thenReturn("ADMIN");

        filter.doFilter(requestWithToken(), new MockHttpServletResponse(), new MockFilterChain());

        CurrentUserPrincipal principal = (CurrentUserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        assertEquals(USER_ID, principal.userId());
    }

    @Test
    void rejectsRevokedSessionSoLoggedOutAccessTokenFailsImmediately() throws Exception {
        UserSession session = UserSession.restore(SESSION_ID, USER_ID, "hash", null,
                NOW.plus(Duration.ofDays(7)), NOW, NOW, NOW);
        JwtAuthenticationFilter filter = configuredFilter(session, activeUser());

        filter.doFilter(requestWithToken(), new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void rejectsExpiredSessionAndLockedUser() throws Exception {
        UserSession expiredSession = UserSession.restore(SESSION_ID, USER_ID, "hash", null,
                NOW.minusSeconds(1), NOW, NOW, null);
        JwtAuthenticationFilter expiredFilter = configuredFilter(expiredSession, activeUser());

        expiredFilter.doFilter(requestWithToken(), new MockHttpServletResponse(), new MockFilterChain());
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        SecurityContextHolder.clearContext();
        UserSession activeSession = UserSession.restore(SESSION_ID, USER_ID, "hash", null,
                NOW.plus(Duration.ofDays(7)), NOW, NOW, null);
        JwtAuthenticationFilter lockedUserFilter = configuredFilter(activeSession, lockedUser());

        lockedUserFilter.doFilter(requestWithToken(), new MockHttpServletResponse(), new MockFilterChain());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void rejectsExpiredAccessTokenBeforeLookingUpSession() throws Exception {
        when(jwtTokenPort.validate("access-token")).thenReturn(false);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                jwtTokenPort, userSessionRepository, userRepository, clockPort
        );

        filter.doFilter(requestWithToken(), new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private JwtAuthenticationFilter configuredFilter(UserSession session, User user) {
        when(jwtTokenPort.validate("access-token")).thenReturn(true);
        when(jwtTokenPort.getUserId("access-token")).thenReturn(USER_ID);
        when(jwtTokenPort.getSessionId("access-token")).thenReturn(SESSION_ID);
        when(userSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(clockPort.now()).thenReturn(NOW);
        return new JwtAuthenticationFilter(jwtTokenPort, userSessionRepository, userRepository, clockPort);
    }

    private MockHttpServletRequest requestWithToken() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/patients");
        request.addHeader("Authorization", "Bearer access-token");
        return request;
    }

    private User activeUser() {
        return User.restore(USER_ID, "admin", "hash", "Admin", "admin@example.com", null,
                UUID.randomUUID(), true, null, NOW);
    }

    private User lockedUser() {
        return User.restore(USER_ID, "admin", "hash", "Admin", "admin@example.com", null,
                UUID.randomUUID(), false, null, NOW);
    }
}

package com.benhsoan.application.ucservice.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.UserSession;
import com.benhsoan.domain.auth.exception.TokenInvalidException;
import com.benhsoan.port.dto.command.auth.RefreshTokenCommand;
import com.benhsoan.port.dto.result.LoginResult;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.authSecurity.RefreshTokenGeneratorPort;
import com.benhsoan.port.outbound.authSecurity.TokenHashPort;
import com.benhsoan.port.outbound.repository.crudRepository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.crudRepository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.crudRepository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-28T04:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ROLE_ID = UUID.randomUUID();

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserSessionRepository userSessionRepository;
    @Mock private JwtTokenPort jwtTokenPort;
    @Mock private TokenHashPort tokenHashPort;
    @Mock private RefreshTokenGeneratorPort refreshTokenGeneratorPort;
    @Mock private ClockPort clockPort;
    @Captor private ArgumentCaptor<UserSession> sessionCaptor;
    @InjectMocks private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        when(clockPort.now()).thenReturn(NOW);
    }

    @Test
    void rotatesCurrentTokenWithoutCreatingANewSession() {
        UserSession session = UserSession.create(USER_ID, "current-hash", NOW.plus(Duration.ofDays(7)));
        User user = User.restore(USER_ID, "admin", "hash", "Admin", "admin@example.com", null,
                ROLE_ID, true, null, NOW.minus(Duration.ofDays(1)));
        Role role = Role.restore(ROLE_ID, "ADMIN", null, true, NOW, NOW, Set.of());

        when(tokenHashPort.hash("current-token")).thenReturn("current-hash");
        when(userSessionRepository.findByRefreshTokenHash("current-hash")).thenReturn(Optional.of(session));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role));
        when(refreshTokenGeneratorPort.generate()).thenReturn("next-token");
        when(tokenHashPort.hash("next-token")).thenReturn("next-hash");
        when(jwtTokenPort.generateToken(USER_ID, session.getId(), "admin", "ADMIN")).thenReturn("access-token");
        when(jwtTokenPort.getExpiredAt("access-token")).thenReturn(NOW.plus(Duration.ofMinutes(15)));
        when(userSessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LoginResult result = service.refreshToken(new RefreshTokenCommand("current-token"));

        verify(userSessionRepository).save(sessionCaptor.capture());
        UserSession savedSession = sessionCaptor.getValue();
        assertEquals(session.getId(), savedSession.getId());
        assertEquals("next-hash", savedSession.getRefreshTokenHash());
        assertEquals("current-hash", savedSession.getPreviousRefreshTokenHash());
        assertEquals(NOW.plus(Duration.ofDays(7)), savedSession.getRefreshExpiresAt());
        assertEquals(NOW, savedSession.getLastUsedAt());
        assertEquals("next-token", result.refreshToken());
    }

    @Test
    void revokesSessionWhenPreviousRefreshTokenIsReused() {
        UserSession session = UserSession.create(USER_ID, "reused-hash", NOW.plus(Duration.ofDays(7)));
        session.rotateRefreshToken("current-hash", NOW.plus(Duration.ofDays(7)), NOW.minus(Duration.ofMinutes(1)));

        when(tokenHashPort.hash("reused-token")).thenReturn("reused-hash");
        when(userSessionRepository.findByRefreshTokenHash("reused-hash")).thenReturn(Optional.empty());
        when(userSessionRepository.findByPreviousRefreshTokenHash("reused-hash")).thenReturn(Optional.of(session));
        when(userSessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(TokenInvalidException.class,
                () -> service.refreshToken(new RefreshTokenCommand("reused-token")));

        verify(userSessionRepository).save(sessionCaptor.capture());
        assertEquals(NOW, sessionCaptor.getValue().getRevokedAt());
        verify(refreshTokenGeneratorPort, never()).generate();
    }

    @Test
    void rejectsUnknownRefreshTokenWithoutMutatingAnySession() {
        when(tokenHashPort.hash("unknown-token")).thenReturn("unknown-hash");
        when(userSessionRepository.findByRefreshTokenHash("unknown-hash")).thenReturn(Optional.empty());
        when(userSessionRepository.findByPreviousRefreshTokenHash("unknown-hash")).thenReturn(Optional.empty());

        assertThrows(TokenInvalidException.class,
                () -> service.refreshToken(new RefreshTokenCommand("unknown-token")));

        verify(userSessionRepository, never()).save(any());
        verify(refreshTokenGeneratorPort, never()).generate();
    }
}

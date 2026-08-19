package com.benhsoan.application.ucservice.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.UserSession;
import com.benhsoan.domain.auth.exception.AccountDisabledException;
import com.benhsoan.domain.auth.exception.SessionExpiredException;
import com.benhsoan.domain.auth.exception.TokenInvalidException;
import com.benhsoan.domain.auth.exception.UserNotFoundException;
import com.benhsoan.port.dto.command.auth.RefreshTokenCommand;
import com.benhsoan.port.dto.result.LoginResult;
import com.benhsoan.port.inbound.auth.RefreshTokenUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.authSecurity.RefreshTokenGeneratorPort;
import com.benhsoan.port.outbound.authSecurity.TokenHashPort;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(noRollbackFor = TokenInvalidException.class)
public class RefreshTokenService implements RefreshTokenUseCase {

    private static final Duration SESSION_TIMEOUT = Duration.ofDays(7);

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final UserSessionRepository userSessionRepository;

    private final JwtTokenPort jwtTokenPort;

    private final TokenHashPort tokenHashPort;

    private final RefreshTokenGeneratorPort refreshTokenGeneratorPort;

    private final ClockPort clockPort;

    @Override
    public LoginResult refreshToken(
            RefreshTokenCommand command
    ) {

        String tokenHash = tokenHashPort.hash(command.refreshToken());

        Instant now = clockPort.now();
        Optional<UserSession> currentSession = userSessionRepository.findByRefreshTokenHash(tokenHash);

        if (currentSession.isEmpty()) {
            UserSession reusedTokenSession = userSessionRepository.findByPreviousRefreshTokenHash(tokenHash)
                    .orElseThrow(TokenInvalidException::new);
            reusedTokenSession.revoke(now);
            userSessionRepository.save(reusedTokenSession);
            throw new TokenInvalidException();
        }

        UserSession session = currentSession.get();

        if (!session.isActive(now, SESSION_TIMEOUT)) {
            throw new SessionExpiredException();
        }

        User user =
                userRepository.findById(session.getUserId())
                        .orElseThrow(UserNotFoundException::new);

        if (!user.isActive()) {
            throw new AccountDisabledException();
        }

        Role role =
                roleRepository.findById(user.getRoleId())
                        .orElseThrow(IllegalStateException::new);

        String refreshToken = refreshTokenGeneratorPort.generate();
        session.rotateRefreshToken(tokenHashPort.hash(refreshToken), now.plus(SESSION_TIMEOUT), now);
        userSessionRepository.save(session);
        String newToken = jwtTokenPort.generateToken(user.getId(), session.getId(), user.getUsername(), role.getName(),
                role.getPermissions().stream().map(permission -> permission.getCode()).collect(java.util.stream.Collectors.toSet()));

        return new LoginResult(
                user.getId(),
                user.getUsername(),
                newToken,
                refreshToken,
                role.getName(),
                jwtTokenPort.getExpiredAt(newToken)
        );
    }
}

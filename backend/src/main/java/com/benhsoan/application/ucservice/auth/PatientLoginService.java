package com.benhsoan.application.ucservice.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.UserSession;
import com.benhsoan.domain.auth.exception.AccountDisabledException;
import com.benhsoan.domain.auth.exception.InvalidCredentialsException;
import com.benhsoan.domain.auth.exception.TooManyLoginAttemptsException;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.port.dto.command.auth.PatientLoginCommand;
import com.benhsoan.port.dto.result.PatientLoginResult;
import com.benhsoan.port.inbound.auth.PatientLoginUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.authSecurity.LoginAttemptPort;
import com.benhsoan.port.outbound.authSecurity.PasswordEncoderPort;
import com.benhsoan.port.outbound.authSecurity.RefreshTokenGeneratorPort;
import com.benhsoan.port.outbound.authSecurity.TokenHashPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * Patient portal authentication (NCL-14-CN-002). Authenticates by phone number + password,
 * enforces consecutive failed-login lockout (TC-02), issues a PATIENT-scoped token carrying
 * the linked patientId (CV-02), and records a login audit with IP/User-Agent (TC-04).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PatientLoginService implements PatientLoginUseCase {

    private static final Duration REFRESH_TOKEN_TIMEOUT = Duration.ofDays(7);
    private static final String PATIENT_ROLE = "PATIENT";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserSessionRepository userSessionRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoderPort passwordEncoderPort;
    private final JwtTokenPort jwtTokenPort;
    private final TokenHashPort tokenHashPort;
    private final RefreshTokenGeneratorPort refreshTokenGeneratorPort;
    private final LoginAttemptPort loginAttemptPort;
    private final AuditLogRepository auditLogRepository;
    private final ClockPort clockPort;
    private final ObjectMapper objectMapper;

    @Override
    public PatientLoginResult login(PatientLoginCommand command) {

        String phone = command.phone();

        if (loginAttemptPort.isBlocked(phone)) {
            throw new TooManyLoginAttemptsException(
                    loginAttemptPort.getRetryAfterSeconds(phone),
                    loginAttemptPort.getBlockedUntil(phone));
        }

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> {
                    loginAttemptPort.loginFailed(phone);
                    return new InvalidCredentialsException();
                });

        if (!user.isActive()) {
            throw new AccountDisabledException();
        }

        if (!passwordEncoderPort.matches(command.password(), user.getPasswordHash())) {
            loginAttemptPort.loginFailed(phone);
            throw new InvalidCredentialsException();
        }

        loginAttemptPort.loginSucceeded(phone);

        Role role = roleRepository.findById(user.getRoleId())
                .orElseThrow(IllegalStateException::new);

        if (!PATIENT_ROLE.equals(role.getName())) {
            throw new InvalidCredentialsException();
        }

        Patient patient = patientRepository.findByUserId(user.getId())
                .orElseThrow(InvalidCredentialsException::new);

        if (!patient.isActive()) {
            throw new InvalidCredentialsException();
        }

        Instant now = clockPort.now();

        userSessionRepository.revokeByUserId(user.getId(), now);

        String refreshToken = refreshTokenGeneratorPort.generate();
        UserSession session = UserSession.create(
                user.getId(),
                tokenHashPort.hash(refreshToken),
                now.plus(REFRESH_TOKEN_TIMEOUT)
        );
        userSessionRepository.save(session);

        String accessToken = jwtTokenPort.generateToken(
                user.getId(),
                session.getId(),
                user.getUsername(),
                role.getName(),
                role.getPermissions().stream().map(permission -> permission.getCode()).collect(java.util.stream.Collectors.toSet()),
                patient.getId()
        );
        Instant expiredAt = jwtTokenPort.getExpiredAt(accessToken);

        user.updateLastLogin(now);
        userRepository.save(user);

        auditLogRepository.save(AuditLog.create(
                user.getId(),
                ActionType.LOGIN,
                ResourceType.PATIENT_PORTAL,
                patient.getId(),
                loginDetail(command, user.getUsername(), patient.getId(), now),
                command.ipAddress(),
                now
        ));

        return new PatientLoginResult(
                user.getId(),
                user.getUsername(),
                accessToken,
                refreshToken,
                role.getName(),
                expiredAt,
                patient.getId()
        );
    }

    private String loginDetail(PatientLoginCommand command, String username, UUID patientId, Instant now) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("username", username);
        detail.put("phone", command.phone());
        detail.put("patientId", patientId.toString());
        detail.put("ipAddress", command.ipAddress());
        detail.put("userAgent", command.userAgent());
        detail.put("loginAt", now.toString());
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize patient login audit detail.", exception);
        }
    }
}

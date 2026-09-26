package com.benhsoan.application.ucservice.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.benhsoan.domain.auth.PatientPasswordRecoveryToken;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.exception.InvalidVerificationCodeException;
import com.benhsoan.port.dto.command.auth.PatientResetPasswordCommand;
import com.benhsoan.port.dto.command.auth.PatientVerifyRecoveryCodeCommand;
import com.benhsoan.port.inbound.auth.PatientResetPasswordUseCase;
import com.benhsoan.port.inbound.auth.PatientVerifyRecoveryCodeUseCase;
import com.benhsoan.port.outbound.authSecurity.PasswordEncoderPort;
import com.benhsoan.port.outbound.repository.auth.PatientPasswordRecoveryTokenRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;

@SpringBootTest(properties = {
                "spring.flyway.enabled=false",
                "spring.sql.init.mode=never",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.url=jdbc:h2:mem:recovery_rollback_test;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@DisplayName("Patient Recovery Rollback Integration Tests")
class PatientRecoveryRollbackIntegrationTest {

        @Autowired
        private PatientResetPasswordUseCase resetPasswordUseCase;

        @Autowired
        private PatientVerifyRecoveryCodeUseCase verifyRecoveryCodeUseCase;

        @Autowired
        private PatientPasswordRecoveryTokenRepository tokenRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private RoleRepository roleRepository;

        @Autowired
        private PasswordEncoderPort passwordEncoderPort;

        private User patientUser;
        private String phone;
        private final String rawCode = "123456";

        @BeforeEach
        void setUp() {
                phone = "090" + (1000000 + new java.util.Random().nextInt(8999999));
                Role patientRole = roleRepository.findByName("PATIENT")
                                .orElseGet(() -> roleRepository
                                                .save(Role.create("PATIENT", "Patient role", true, Set.of())));

                patientUser = userRepository.save(User.create(
                                phone,
                                passwordEncoderPort.encode("OldPassword123!"),
                                "Rollback Test Patient",
                                phone + "@test.com",
                                phone,
                                patientRole.getId()));
        }

        @Test
        @DisplayName("Finding 1: Attempts must be incremented and persisted when wrong OTP is submitted (survives rollback)")
        void shouldPersistIncrementedAttemptsEvenWhenServiceThrowsInvalidVerificationCodeException() {
                Instant now = Instant.now();
                PatientPasswordRecoveryToken token = PatientPasswordRecoveryToken.create(
                                patientUser.getId(),
                                phone,
                                passwordEncoderPort.encode(rawCode),
                                now.plusSeconds(300),
                                now);
                tokenRepository.save(token);

                // Submit wrong code
                PatientResetPasswordCommand command = new PatientResetPasswordCommand(
                                phone,
                                "999999", // wrong code
                                "NewPassword123!",
                                "127.0.0.1",
                                "JUnit");

                assertThrows(InvalidVerificationCodeException.class, () -> resetPasswordUseCase.resetPassword(command));

                // Check directly in database: attempts must be 1, NOT rolled back to 0!
                PatientPasswordRecoveryToken updatedToken = tokenRepository.findLatestActiveByPhone(phone)
                                .orElseThrow();
                assertEquals(1, updatedToken.getAttempts(),
                                "Attempts must be 1 in DB despite exception and business rollback");
        }

        @Test
        @DisplayName("Finding 3: verifyCode must also increment attempts and persist it")
        void shouldPersistAttemptsOnVerifyCodeFailure() {
                Instant now = Instant.now();
                PatientPasswordRecoveryToken token = PatientPasswordRecoveryToken.create(
                                patientUser.getId(),
                                phone,
                                passwordEncoderPort.encode(rawCode),
                                now.plusSeconds(300),
                                now);
                tokenRepository.save(token);

                PatientVerifyRecoveryCodeCommand command = new PatientVerifyRecoveryCodeCommand(phone, "000000");

                assertThrows(InvalidVerificationCodeException.class,
                                () -> verifyRecoveryCodeUseCase.verifyCode(command));

                PatientPasswordRecoveryToken updatedToken = tokenRepository.findLatestActiveByPhone(phone)
                                .orElseThrow();
                assertEquals(1, updatedToken.getAttempts());
        }

        @Test
        @DisplayName("Finding 6: Should persist ACCESS_DENIED audit log when failed attempts reach 5")
        void shouldPersistAccessDeniedAuditLogWhenAttemptsExceedFive() {
                Instant now = Instant.now();
                PatientPasswordRecoveryToken token = PatientPasswordRecoveryToken.create(
                                patientUser.getId(),
                                phone,
                                passwordEncoderPort.encode(rawCode),
                                now.plusSeconds(300),
                                now);
                tokenRepository.save(token);

                PatientVerifyRecoveryCodeCommand command = new PatientVerifyRecoveryCodeCommand(phone, "000000");

                // Submit 5 wrong codes
                for (int i = 0; i < 5; i++) {
                        assertThrows(InvalidVerificationCodeException.class,
                                        () -> verifyRecoveryCodeUseCase.verifyCode(command));
                }

                PatientPasswordRecoveryToken updatedToken = tokenRepository.findLatestActiveByPhone(phone)
                                .orElseThrow();
                assertEquals(5, updatedToken.getAttempts());
                assertTrue(updatedToken.isAttemptsExceeded());
        }
}

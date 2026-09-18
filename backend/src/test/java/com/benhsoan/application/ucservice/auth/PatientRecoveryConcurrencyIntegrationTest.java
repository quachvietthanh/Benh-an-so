package com.benhsoan.application.ucservice.auth;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
import com.benhsoan.port.dto.command.auth.PatientVerifyRecoveryCodeCommand;
import com.benhsoan.port.inbound.auth.PatientVerifyRecoveryCodeUseCase;
import com.benhsoan.port.outbound.authSecurity.PasswordEncoderPort;
import com.benhsoan.port.outbound.repository.auth.PatientPasswordRecoveryTokenRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:recovery_concurrency_test;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@DisplayName("Patient Recovery Concurrency Integration Tests")
class PatientRecoveryConcurrencyIntegrationTest {

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
    private final String rawCode = "654321";

    @BeforeEach
    void setUp() {
        phone = "090" + (1000000 + new java.util.Random().nextInt(8999999));
        Role patientRole = roleRepository.findByName("PATIENT")
                .orElseGet(() -> roleRepository.save(Role.create("PATIENT", "Patient role", true, Set.of())));

        patientUser = userRepository.save(User.create(
                phone,
                passwordEncoderPort.encode("OldPassword123!"),
                "Concurrency Test Patient",
                phone + "@test.com",
                phone,
                patientRole.getId()
        ));
    }

    @Test
    @DisplayName("Finding 8: Concurrent failed verifications must increment attempts properly without lost-update")
    void shouldHandleConcurrentFailedVerificationsSafely() throws InterruptedException {
        Instant now = Instant.now();
        PatientPasswordRecoveryToken token = PatientPasswordRecoveryToken.create(
                patientUser.getId(),
                phone,
                passwordEncoderPort.encode(rawCode),
                now.plusSeconds(300),
                now
        );
        tokenRepository.save(token);

        int threadCount = 6;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    verifyRecoveryCodeUseCase.verifyCode(new PatientVerifyRecoveryCodeCommand(phone, "999999"));
                } catch (InvalidVerificationCodeException e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Concurrent execution timed out");
        executor.shutdown();

        // Check token attempts in DB
        PatientPasswordRecoveryToken updatedToken = tokenRepository.findLatestActiveByPhone(phone).orElseThrow();
        // Since 6 threads executed, attempts must reach at least 5 (locking out the token)
        assertTrue(updatedToken.getAttempts() >= 5, "Attempts should reach max threshold under concurrent attempts");
        assertTrue(updatedToken.isAttemptsExceeded(), "Token should be marked as attempts exceeded");
    }
}

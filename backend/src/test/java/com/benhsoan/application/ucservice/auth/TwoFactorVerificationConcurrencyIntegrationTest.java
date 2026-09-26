package com.benhsoan.application.ucservice.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.TwoFactorChallenge;
import com.benhsoan.domain.auth.User;
import com.benhsoan.port.dto.command.auth.VerifyTwoFactorCommand;
import com.benhsoan.port.inbound.auth.TwoFactorVerificationUseCase;
import com.benhsoan.port.outbound.authSecurity.PasswordEncoderPort;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.TwoFactorChallengeRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:twofactor_concurrency_test;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@DisplayName("Two-Factor Verification Concurrency Integration Tests")
class TwoFactorVerificationConcurrencyIntegrationTest {

    @Autowired
    private TwoFactorVerificationUseCase verificationUseCase;

    @Autowired
    private TwoFactorChallengeRepository challengeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoderPort passwordEncoderPort;

    private User user;
    private TwoFactorChallenge challenge;
    private final String rawCode = "123456";

    @BeforeEach
    void setUp() {
        Role doctorRole = roleRepository.findByName("DOCTOR")
                .orElseGet(() -> roleRepository.save(Role.create("DOCTOR", "Doctor role", true, Set.of())));
        user = userRepository.save(User.create(
                "twofactor_concurrency_user",
                passwordEncoderPort.encode("CorrectPassword123!"),
                "Concurrency Doctor",
                "twofactor_concurrency@test.com",
                null,
                doctorRole.getId()
        ));

        Instant now = Instant.now();
        challenge = challengeRepository.save(TwoFactorChallenge.create(
                user.getId(),
                passwordEncoderPort.encode(rawCode),
                now.plusSeconds(300),
                now
        ));
    }

    @Test
    @DisplayName("Concurrent verification of the same challenge: exactly one succeeds and the challenge is consumed once")
    void concurrentVerificationExactlyOneSucceeds() throws InterruptedException {
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await(10, TimeUnit.SECONDS);
                    verificationUseCase.verify(new VerifyTwoFactorCommand(challenge.getId().toString(), rawCode, "1.2.3.4"));
                    successCount.incrementAndGet();
                } catch (Exception ex) {
                    failureCount.incrementAndGet();
                }
                return null;
            });
        }

        assert ready.await(10, TimeUnit.SECONDS) : "Threads did not reach ready state in time";
        start.countDown();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        assertEquals(1, successCount.get(), "Exactly one verification must succeed");
        assertEquals(1, failureCount.get(), "Exactly one verification must fail");

        // The challenge is consumed exactly once (fresh DB read shows consumedAt set).
        assertTrue(challengeRepository.findById(challenge.getId()).orElseThrow().isConsumed());
    }
}

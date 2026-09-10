package com.benhsoan.infrastructure.authSecurity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.exception.InvalidCredentialsException;
import com.benhsoan.domain.auth.exception.TooManyLoginAttemptsException;
import com.benhsoan.port.dto.command.auth.LoginCommand;
import com.benhsoan.port.dto.result.LoginAttemptResult;
import com.benhsoan.port.dto.result.LoginAuditLogResult;
import com.benhsoan.port.inbound.auth.LoginUseCase;
import com.benhsoan.port.inbound.user.GetLoginAuditLogsUseCase;
import com.benhsoan.port.outbound.authSecurity.LoginAttemptPort;
import com.benhsoan.port.outbound.authSecurity.PasswordEncoderPort;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:concurrency_test;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
class LoginAttemptConcurrencyIntegrationTest {

    @Autowired
    private LoginAttemptPort loginAttemptPort;

    @Autowired
    private LoginUseCase loginUseCase;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoderPort passwordEncoderPort;

    @Autowired
    private GetLoginAuditLogsUseCase getLoginAuditLogsUseCase;

    private static final String CONCURRENT_USER = "concurrent_failed_user";
    private static final String CONCURRENT_E2E_USER = "concurrent_e2e_user";
    private static final String WRONG_PASSWORD = "WrongPassword123!";

    private User e2eUser;

    @BeforeEach
    void setUp() {
        loginAttemptPort.unlock(CONCURRENT_USER);
        loginAttemptPort.unlock(CONCURRENT_E2E_USER);

        Role doctorRole = roleRepository.findByName("DOCTOR")
                .orElseGet(() -> roleRepository.save(Role.create("DOCTOR", "Doctor role", true, Set.of())));

        e2eUser = userRepository.findByUsername(CONCURRENT_E2E_USER).orElseGet(() -> {
            User user = User.create(
                    CONCURRENT_E2E_USER,
                    passwordEncoderPort.encode("CorrectPassword123!"),
                    "Concurrent Test User",
                    "concurrent@hospital.vn",
                    "0908888777",
                    doctorRole.getId());
            return userRepository.save(user);
        });
    }

    @AfterEach
    void tearDown() {
        loginAttemptPort.unlock(CONCURRENT_USER);
        loginAttemptPort.unlock(CONCURRENT_E2E_USER);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("P0 Concurrency: 8 luồng đồng thời gọi recordLoginFailed -> Counter đạt đúng 8, chỉ duy nhất 1 luồng nhận newlyBlocked=true")
    void concurrentFailedAttempts_allIncrementAtomically_andExactlyOneGetsNewlyBlocked() throws Exception {
        int threadCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Callable<LoginAttemptResult>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> {
                readyLatch.countDown();
                startLatch.await();
                return loginAttemptPort.recordLoginFailed(CONCURRENT_USER);
            });
        }

        List<Future<LoginAttemptResult>> futures = new ArrayList<>();
        for (Callable<LoginAttemptResult> task : tasks) {
            futures.add(executor.submit(task));
        }

        assertTrue(readyLatch.await(5, TimeUnit.SECONDS), "All threads should be ready");
        startLatch.countDown();

        List<LoginAttemptResult> results = new ArrayList<>();
        for (Future<LoginAttemptResult> future : futures) {
            results.add(future.get(10, TimeUnit.SECONDS));
        }
        executor.shutdown();

        // 1. Không bị lost update: Đếm cuối cùng phải chính xác là 8
        assertEquals(8, loginAttemptPort.getAttemptCount(CONCURRENT_USER), "Final attempt count must be 8");
        assertTrue(loginAttemptPort.isBlocked(CONCURRENT_USER), "Account must be blocked");
        assertNotNull(loginAttemptPort.getBlockedUntil(CONCURRENT_USER), "Blocked until must be set");

        // 2. Chỉ duy nhất 1 luồng nhận newlyBlocked = true (chính là luồng chạm đúng
        // mốc maxAttempts = 5)
        long newlyBlockedCount = results.stream().filter(LoginAttemptResult::newlyBlocked).count();
        assertEquals(1, newlyBlockedCount, "Exactly ONE thread must get newlyBlocked=true");

        // 3. Đúng 4 luồng nhận blocked = true (các luồng có attempt >= 5: 5, 6, 7, 8)
        long blockedCount = results.stream().filter(LoginAttemptResult::blocked).count();
        assertEquals(4, blockedCount, "4 threads must receive blocked=true");

        // 4. Đúng 4 luồng nhận blocked = false (các luồng có attempt < 5: 1, 2, 3, 4)
        long unblockedCount = results.stream().filter(r -> !r.blocked()).count();
        assertEquals(4, unblockedCount, "4 threads must receive blocked=false");
    }

    @Test
    @DisplayName("P0 Concurrency: 8 luồng đăng nhập sai đồng thời qua LoginService -> Chỉ duy nhất 1 bản ghi Audit Log LOCK được sinh ra")
    void concurrentLoginRequests_recordsSingleLockAuditLog() throws Exception {
        int threadCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Callable<Exception>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> {
                readyLatch.countDown();
                startLatch.await();
                try {
                    loginUseCase.login(new LoginCommand(CONCURRENT_E2E_USER, WRONG_PASSWORD));
                    return null;
                } catch (Exception ex) {
                    return ex;
                }
            });
        }

        List<Future<Exception>> futures = new ArrayList<>();
        for (Callable<Exception> task : tasks) {
            futures.add(executor.submit(task));
        }

        assertTrue(readyLatch.await(5, TimeUnit.SECONDS), "All threads should be ready");
        startLatch.countDown();

        int invalidCredentialsCount = 0;
        int tooManyAttemptsCount = 0;
        for (Future<Exception> future : futures) {
            Exception ex = future.get(10, TimeUnit.SECONDS);
            if (ex instanceof InvalidCredentialsException) {
                invalidCredentialsCount++;
            } else if (ex instanceof TooManyLoginAttemptsException) {
                tooManyAttemptsCount++;
            }
        }
        executor.shutdown();

        // Cả 8 request đều phải bị từ chối
        assertEquals(4, invalidCredentialsCount,
                "4 requests under threshold must fail with InvalidCredentialsException");
        assertEquals(4, tooManyAttemptsCount,
                "4 requests at or above threshold must fail with TooManyLoginAttemptsException");

        // Kiểm tra số lượng Audit Log LOCK trong database: CHÍNH XÁC LÀ 1!
        Page<LoginAuditLogResult> auditLogs = getLoginAuditLogsUseCase.getLoginAuditLogs(e2eUser.getId(),
                PageRequest.of(0, 20));
        long lockLogCount = auditLogs.getContent().stream()
                .filter(log -> log.actionType() == ActionType.LOCK)
                .count();

        assertEquals(1, lockLogCount, "Must have exactly ONE LOCK audit log despite concurrent failed login requests!");
    }
}

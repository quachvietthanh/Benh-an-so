package com.benhsoan.application.ucservice.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.exception.InvalidCredentialsException;
import com.benhsoan.domain.auth.exception.TooManyLoginAttemptsException;
import com.benhsoan.infrastructure.authSecurity.CurrentUserPrincipal;
import com.benhsoan.port.dto.command.auth.LoginCommand;
import com.benhsoan.port.dto.result.LoginAuditLogResult;
import com.benhsoan.port.dto.result.LoginResult;
import com.benhsoan.port.inbound.auth.LoginUseCase;
import com.benhsoan.port.inbound.user.GetLoginAuditLogsUseCase;
import com.benhsoan.port.inbound.user.UnlockUserUseCase;
import com.benhsoan.port.outbound.authSecurity.LoginAttemptPort;
import com.benhsoan.port.outbound.authSecurity.PasswordEncoderPort;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:lockout_integration_test;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
class LoginServiceLockoutIntegrationTest {

    @Autowired
    private LoginUseCase loginUseCase;

    @Autowired
    private UnlockUserUseCase unlockUserUseCase;

    @Autowired
    private GetLoginAuditLogsUseCase getLoginAuditLogsUseCase;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoderPort passwordEncoderPort;

    @Autowired
    private LoginAttemptPort loginAttemptPort;

    private User testUser;
    private UUID testUserId;
    private static final String USERNAME = "lockout_test_user";
    private static final String PASSWORD = "CorrectPassword123!";
    private static final String WRONG_PASSWORD = "WrongPassword123!";
    private static final UUID ADMIN_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");

    @BeforeEach
    void setUp() {
        loginAttemptPort.unlock(USERNAME);

        Role doctorRole = roleRepository.findByName("DOCTOR").orElseGet(() ->
                roleRepository.save(Role.create("DOCTOR", "Doctor role", true, Set.of()))
        );

        Role adminRole = roleRepository.findByName("ADMIN").orElseGet(() ->
                roleRepository.save(Role.create("ADMIN", "Admin role", true, Set.of()))
        );

        testUser = userRepository.findByUsername(USERNAME).orElseGet(() -> {
            User newUser = User.create(
                    USERNAME,
                    passwordEncoderPort.encode(PASSWORD),
                    "Lockout Test Doctor",
                    "lockout@hospital.vn",
                    "0909999888",
                    doctorRole.getId()
            );
            return userRepository.save(newUser);
        });
        testUserId = testUser.getId();

        userRepository.findById(ADMIN_ID).orElseGet(() -> {
            User adminUser = User.restore(
                    ADMIN_ID,
                    "admin",
                    passwordEncoderPort.encode("admin123"),
                    "System Admin",
                    "admin@hospital.vn",
                    "0901111222",
                    adminRole.getId(),
                    true,
                    false,
                    null,
                    Instant.now()
            );
            return userRepository.save(adminUser);
        });
    }

    @AfterEach
    void tearDown() {
        loginAttemptPort.unlock(USERNAME);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("NCL-01-CN-008 End-to-End Lockout & Audit Log Persistence Across Transaction Rollback")
    void lockoutAuditLogSurvivesRollback_andEarlyUnlockWorks() {
        // 1. Nhập sai mật khẩu 4 lần đầu -> ném InvalidCredentialsException
        for (int i = 1; i <= 4; i++) {
            assertThrows(InvalidCredentialsException.class, () ->
                    loginUseCase.login(new LoginCommand(USERNAME, WRONG_PASSWORD))
            );
            assertFalse(loginAttemptPort.isBlocked(USERNAME));
        }

        // 2. Nhập sai lần thứ 5 -> ném TooManyLoginAttemptsException (TC-01)
        TooManyLoginAttemptsException lockoutEx = assertThrows(
                TooManyLoginAttemptsException.class,
                () -> loginUseCase.login(new LoginCommand(USERNAME, WRONG_PASSWORD))
        );
        assertTrue(lockoutEx.getRetryAfterSeconds() > 0);
        assertTrue(loginAttemptPort.isBlocked(USERNAME));

        // 3. XÁC MINH BUG P0 ĐÃ ĐƯỢC SỬA:
        // Dù TooManyLoginAttemptsException bị ném ra, bản ghi LOCK PHẢI TỒN TẠI trong Database (audit_logs)
        Page<LoginAuditLogResult> auditLogs = getLoginAuditLogsUseCase.getLoginAuditLogs(testUserId, PageRequest.of(0, 10));
        assertFalse(auditLogs.isEmpty(), "Audit log LOCK must be persisted in DB despite the exception rollback!");
        LoginAuditLogResult lockLog = auditLogs.getContent().stream()
                .filter(log -> log.actionType() == ActionType.LOCK)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected LOCK audit log not found in database"));
        assertEquals(testUserId, lockLog.resourceId());
        assertEquals(ResourceType.USER, lockLog.resourceType());
        assertTrue(lockLog.detail().contains("failedAttempts"));

        // 4. TC-02: Nhập đúng mật khẩu khi tài khoản đang bị khóa tạm -> vẫn bị từ chối
        assertThrows(TooManyLoginAttemptsException.class, () ->
                loginUseCase.login(new LoginCommand(USERNAME, PASSWORD))
        );

        // 5. Quản trị viên (Admin) mở khóa tài khoản sớm (Early Unlock)
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new CurrentUserPrincipal(ADMIN_ID, "admin"),
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_ADMIN"),
                                new SimpleGrantedAuthority("PERMISSION_USER_UPDATE"),
                                new SimpleGrantedAuthority("PERMISSION_USER_READ")
                        )
                )
        );

        unlockUserUseCase.unlockUser(testUserId);
        assertFalse(loginAttemptPort.isBlocked(USERNAME));

        // 6. Sau khi mở khóa, người dùng đăng nhập thành công với mật khẩu đúng (TC-03)
        LoginResult loginResult = loginUseCase.login(new LoginCommand(USERNAME, PASSWORD));
        assertNotNull(loginResult);
        assertNotNull(loginResult.accessToken());

        // 7. XÁC MINH BUG P1 ĐÃ ĐƯỢC SỬA:
        // Tra cứu nhật ký của user bị khóa: có cả bản ghi LOCK và UNLOCK
        Page<LoginAuditLogResult> userLogsAfterUnlock = getLoginAuditLogsUseCase.getLoginAuditLogs(testUserId, PageRequest.of(0, 10));
        assertTrue(userLogsAfterUnlock.getContent().stream().anyMatch(l -> l.actionType() == ActionType.UNLOCK));

        // Tra cứu nhật ký của Admin: không được lẫn bản ghi UNLOCK của user khác
        Page<LoginAuditLogResult> adminLogs = getLoginAuditLogsUseCase.getLoginAuditLogs(ADMIN_ID, PageRequest.of(0, 10));
        boolean adminContainsOtherUserUnlock = adminLogs.getContent().stream()
                .anyMatch(l -> l.actionType() == ActionType.UNLOCK && testUserId.equals(l.resourceId()));
        assertFalse(adminContainsOtherUserUnlock, "Admin's personal login logs must not include UNLOCK actions performed on other users!");
    }
}

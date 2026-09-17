package com.benhsoan.application.ucservice.user;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.benhsoan.application.ucservice.auditlog.AdminOperationAuditService;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.persistence.entity.auth.LoginAttemptEntity;
import com.benhsoan.persistence.jpaRepository.auditlog.JpaAuditLogRepository;
import com.benhsoan.persistence.jpaRepository.auth.JpaLoginAttemptRepository;
import com.benhsoan.port.inbound.user.UnlockUserUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:unlock_atomicity_test;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
class UnlockUserServiceTransactionAtomicityIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID ADMIN_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");
    private static final UUID USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID ROLE_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final String USERNAME = "atomicity_test_user";

    @Autowired private UnlockUserUseCase unlockUserUseCase;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private JpaLoginAttemptRepository jpaLoginAttemptRepository;
    @Autowired private JpaAuditLogRepository jpaAuditLogRepository;

    @MockitoSpyBean private AdminOperationAuditService adminOperationAuditService;
    @MockitoBean private CurrentUserPort currentUserPort;

    @BeforeEach
    void setUp() {
        jpaLoginAttemptRepository.deleteAll();
        jpaAuditLogRepository.deleteAll();
        when(currentUserPort.getCurrentUserId()).thenReturn(ADMIN_ID);
        if (roleRepository.findById(ROLE_ID).isEmpty()) {
            roleRepository.save(Role.restore(ROLE_ID, "DOCTOR", "Doctor", true, NOW, NOW, Set.of()));
        }
        if (userRepository.findById(USER_ID).isEmpty()) {
            userRepository.save(User.restore(USER_ID, USERNAME, "hash", "Doctor One", "d@x.com", "0901234567", ROLE_ID, true, null, NOW));
        }
    }

    private void seedBlockedAttempt() {
        jpaLoginAttemptRepository.save(LoginAttemptEntity.builder()
                .identifier(USERNAME)
                .attempts(5)
                .blockedUntil(Instant.now().plusSeconds(3600))
                .updatedAt(NOW)
                .build());
    }

    @Test
    void successfulUnlockRemovesAttemptAndCommitsAuditAtomically() {
        seedBlockedAttempt();

        unlockUserUseCase.unlockUser(USER_ID);

        assertTrue(jpaLoginAttemptRepository.findById(USERNAME).isEmpty(),
                "Successful unlock must remove the login attempt");
        List<AuditLog> audits = auditLogRepository.findByResourceTypeAndResourceId(ResourceType.USER, USER_ID);
        assertTrue(audits.stream().anyMatch(audit -> audit.getActionType() == ActionType.UNLOCK),
                "Successful unlock must persist an UNLOCK audit record");
    }

    @Test
    void auditFailureRollsBackLoginAttemptDeletion() {
        seedBlockedAttempt();
        doThrow(new IllegalStateException("Simulated audit persistence failure"))
                .when(adminOperationAuditService).record(any(), any(), any(), any(), any(), any(), any());

        assertThrows(IllegalStateException.class, () -> unlockUserUseCase.unlockUser(USER_ID));

        assertTrue(jpaLoginAttemptRepository.findById(USERNAME).isPresent(),
                "Login attempt deletion must be rolled back when the audit cannot be recorded (QTN-31)");
        List<AuditLog> audits = auditLogRepository.findByResourceTypeAndResourceId(ResourceType.USER, USER_ID);
        assertTrue(audits.stream().noneMatch(audit -> audit.getActionType() == ActionType.UNLOCK),
                "No successful UNLOCK audit may exist for the failed transaction");
    }
}

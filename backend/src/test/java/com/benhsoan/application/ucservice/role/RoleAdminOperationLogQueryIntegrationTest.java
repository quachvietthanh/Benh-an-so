package com.benhsoan.application.ucservice.role;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.persistence.entity.auth.PermissionEntity;
import com.benhsoan.persistence.entity.auth.RoleEntity;
import com.benhsoan.persistence.jpaRepository.auditlog.JpaAuditLogRepository;
import com.benhsoan.persistence.jpaRepository.auth.JpaPermissionRepository;
import com.benhsoan.persistence.jpaRepository.auth.JpaRoleRepository;
import com.benhsoan.port.dto.command.auditlog.AdminOperationLogQuery;
import com.benhsoan.port.dto.command.role.UpdateRolePermissionsCommand;
import com.benhsoan.port.dto.result.auditlog.AdminOperationLogResult;
import com.benhsoan.port.inbound.auditlog.GetAdminOperationLogsUseCase;
import com.benhsoan.port.inbound.role.UpdateRolePermissionsUseCase;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:role_admin_log_query_test;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
class RoleAdminOperationLogQueryIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");
    private static final UUID ACTOR_ROLE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID TARGET_ROLE_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID ROLE_READ_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-ddddddddddd1");
    private static final UUID ROLE_UPDATE_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-ddddddddddd2");
    private static final UUID PERMISSION_READ_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-ddddddddddd3");

    @Autowired private UpdateRolePermissionsUseCase updateRolePermissionsUseCase;
    @Autowired private GetAdminOperationLogsUseCase getAdminOperationLogsUseCase;
    @Autowired private UserRepository userRepository;
    @Autowired private JpaPermissionRepository jpaPermissionRepository;
    @Autowired private JpaRoleRepository jpaRoleRepository;
    @Autowired private JpaAuditLogRepository jpaAuditLogRepository;

    @MockitoBean private CurrentUserPort currentUserPort;

    @BeforeEach
    void setUp() {
        jpaAuditLogRepository.deleteAll();
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);

        if (jpaPermissionRepository.findById(ROLE_READ_ID).isEmpty()) {
            jpaPermissionRepository.saveAll(List.of(
                    permission(ROLE_READ_ID, "ROLE_READ"),
                    permission(ROLE_UPDATE_ID, "ROLE_UPDATE"),
                    permission(PERMISSION_READ_ID, "PERMISSION_READ")));
        }
        if (jpaRoleRepository.findById(TARGET_ROLE_ID).isEmpty()) {
            PermissionEntity roleRead = jpaPermissionRepository.findById(ROLE_READ_ID).orElseThrow();
            jpaRoleRepository.save(RoleEntity.builder()
                    .id(TARGET_ROLE_ID).name("DOCTOR").description("Doctor").isSystem(true)
                    .createdAt(NOW).updatedAt(NOW)
                    .permissions(new HashSet<>(Set.of(roleRead)))
                    .build());
        }
        if (jpaRoleRepository.findById(ACTOR_ROLE_ID).isEmpty()) {
            jpaRoleRepository.save(RoleEntity.builder()
                    .id(ACTOR_ROLE_ID).name("ADMIN").description("Admin").isSystem(true)
                    .createdAt(NOW).updatedAt(NOW)
                    .permissions(new HashSet<>())
                    .build());
        }
        if (userRepository.findById(ACTOR_ID).isEmpty()) {
            userRepository.save(User.restore(ACTOR_ID, "admin", "hash", "Admin", "admin@example.com", null,
                    ACTOR_ROLE_ID, true, null, NOW));
        }
    }

    @Test
    void rolePermissionUpdateIsQueryableWithStandardizedDetail() throws Exception {
        updateRolePermissionsUseCase.updateRolePermissions(new UpdateRolePermissionsCommand(
                TARGET_ROLE_ID, List.of("ROLE_READ", "ROLE_UPDATE", "PERMISSION_READ")));

        var page = getAdminOperationLogsUseCase.getLogs(
                new AdminOperationLogQuery(null, ResourceType.ROLE, null, null), PageRequest.of(0, 20));

        AdminOperationLogResult roleLog = page.getContent().stream()
                .filter(log -> log.actionType() == ActionType.UPDATE
                        && log.resourceType() == ResourceType.ROLE
                        && TARGET_ROLE_ID.equals(log.resourceId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected a ROLE UPDATE audit record"));

        JsonNode detail = new ObjectMapper().readTree(roleLog.detail());
        JsonNode before = detail.path("before");
        JsonNode after = detail.path("after");
        assertEquals("DOCTOR", before.path("roleName").asText());
        assertEquals("DOCTOR", after.path("roleName").asText());
        assertTrue(before.path("permissions").isArray(), "before.permissions must be a JSON array");
        assertTrue(after.path("permissions").isArray(), "after.permissions must be a JSON array");
        assertEquals("ROLE_READ", before.path("permissions").get(0).asText());
        assertEquals("PERMISSION_READ", after.path("permissions").get(0).asText());
        assertEquals("ROLE_READ", after.path("permissions").get(1).asText());
        assertEquals("ROLE_UPDATE", after.path("permissions").get(2).asText());
    }

    private PermissionEntity permission(UUID id, String code) {
        return PermissionEntity.builder()
                .id(id).code(code).name(code).module("TEST").description(null)
                .active(true).createdAt(NOW).updatedAt(NOW)
                .build();
    }
}

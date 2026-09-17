package com.benhsoan.persistence.adapterRepository.auditlog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.persistence.jpaRepository.auditlog.JpaAuditLogRepository;
import com.benhsoan.persistence.mapper.auditlog.AuditLogPersistenceMapper;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({AuditLogRepositoryAdapter.class, AuditLogPersistenceMapper.class})
class AuditLogRepositoryAdapterIntegrationTest {

    @Autowired
    private AuditLogRepository repository;

    @Autowired
    private JpaAuditLogRepository jpaRepository;

    @BeforeEach
    void cleanUp() {
        jpaRepository.deleteAll();
    }

    @Test
    void findAdminOperationLogsFiltersByActorAndResourceTypeAndExcludesLoginActivity() {
        UUID actorA = UUID.randomUUID();
        UUID actorB = UUID.randomUUID();
        Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t2 = Instant.parse("2026-01-02T00:00:00Z");
        Instant t3 = Instant.parse("2026-01-03T00:00:00Z");

        repository.save(AuditLog.create(actorA, ActionType.CREATE, ResourceType.SERVICE_PRICE,
                UUID.randomUUID(), null, null, t3));
        repository.save(AuditLog.create(actorA, ActionType.CREATE, ResourceType.MEDICINE,
                UUID.randomUUID(), null, null, t2));
        repository.save(AuditLog.create(actorB, ActionType.UPDATE, ResourceType.USER,
                UUID.randomUUID(), null, null, t1));
        // Login activity on USER must be excluded from the administrative log.
        repository.save(AuditLog.create(actorA, ActionType.LOGIN, ResourceType.USER,
                UUID.randomUUID(), null, null, t2));
        // Non-administrative resource type must be excluded.
        repository.save(AuditLog.create(actorA, ActionType.READ, ResourceType.PATIENT,
                UUID.randomUUID(), null, null, t1));

        var all = repository.findAdminOperationLogs(null, null, null, null, PageRequest.of(0, 20));
        assertEquals(3, all.getTotalElements());

        var byActor = repository.findAdminOperationLogs(actorA, null, null, null, PageRequest.of(0, 20));
        assertEquals(2, byActor.getTotalElements());
        assertTrue(byActor.getContent().stream().allMatch(log -> actorA.equals(log.getUserId())));

        var byResourceType = repository.findAdminOperationLogs(null, ResourceType.MEDICINE, null, null,
                PageRequest.of(0, 20));
        assertEquals(1, byResourceType.getTotalElements());
        assertEquals(ResourceType.MEDICINE, byResourceType.getContent().get(0).getResourceType());

        // Sorted newest first.
        assertEquals(ResourceType.SERVICE_PRICE, all.getContent().get(0).getResourceType());
    }

    @Test
    void excludesNonAdministrativeSecurityEventsFromAdminOperationLog() {
        UUID actor = UUID.randomUUID();
        Instant t = Instant.parse("2026-01-01T00:00:00Z");

        // These share USER/PERMISSION resource types but are not administrative configuration operations.
        repository.save(AuditLog.create(actor, ActionType.ACCESS_DENIED, ResourceType.PERMISSION,
                null, null, null, t));
        repository.save(AuditLog.create(actor, ActionType.LOCK, ResourceType.USER,
                UUID.randomUUID(), null, null, t));
        repository.save(AuditLog.create(actor, ActionType.CHANGE_PASSWORD, ResourceType.USER,
                UUID.randomUUID(), null, null, t));
        repository.save(AuditLog.create(actor, ActionType.RESET_PASSWORD, ResourceType.USER,
                UUID.randomUUID(), null, null, t));

        var result = repository.findAdminOperationLogs(null, null, null, null, PageRequest.of(0, 20));

        assertEquals(0, result.getTotalElements(),
                "ACCESS_DENIED/LOCK/CHANGE_PASSWORD/RESET_PASSWORD must not appear in the administrative log");
    }

    @Test
    void includesAllLegitimateAdministrativeOperations() {
        UUID actor = UUID.randomUUID();
        Instant t = Instant.parse("2026-01-01T00:00:00Z");

        repository.save(AuditLog.create(actor, ActionType.CREATE, ResourceType.USER, UUID.randomUUID(), null, null, t));
        repository.save(AuditLog.create(actor, ActionType.UPDATE, ResourceType.USER, UUID.randomUUID(), null, null, t));
        repository.save(AuditLog.create(actor, ActionType.ACTIVATE, ResourceType.USER, UUID.randomUUID(), null, null, t));
        repository.save(AuditLog.create(actor, ActionType.DEACTIVATE, ResourceType.USER, UUID.randomUUID(), null, null, t));
        repository.save(AuditLog.create(actor, ActionType.UNLOCK, ResourceType.USER, UUID.randomUUID(), null, null, t));
        repository.save(AuditLog.create(actor, ActionType.UPDATE, ResourceType.ROLE, UUID.randomUUID(), null, null, t));
        repository.save(AuditLog.create(actor, ActionType.CREATE, ResourceType.MEDICINE, UUID.randomUUID(), null, null, t));
        repository.save(AuditLog.create(actor, ActionType.UPDATE, ResourceType.MEDICINE, UUID.randomUUID(), null, null, t));
        repository.save(AuditLog.create(actor, ActionType.ACTIVATE, ResourceType.MEDICINE, UUID.randomUUID(), null, null, t));
        repository.save(AuditLog.create(actor, ActionType.DEACTIVATE, ResourceType.MEDICINE, UUID.randomUUID(), null, null, t));
        repository.save(AuditLog.create(actor, ActionType.CREATE, ResourceType.SERVICE_CATALOG, UUID.randomUUID(), null, null, t));
        repository.save(AuditLog.create(actor, ActionType.UPDATE, ResourceType.SERVICE_CATALOG, UUID.randomUUID(), null, null, t));
        repository.save(AuditLog.create(actor, ActionType.ACTIVATE, ResourceType.SERVICE_CATALOG, UUID.randomUUID(), null, null, t));
        repository.save(AuditLog.create(actor, ActionType.DEACTIVATE, ResourceType.SERVICE_CATALOG, UUID.randomUUID(), null, null, t));
        repository.save(AuditLog.create(actor, ActionType.CREATE, ResourceType.SERVICE_PRICE, UUID.randomUUID(), null, null, t));

        var result = repository.findAdminOperationLogs(null, null, null, null, PageRequest.of(0, 20));

        assertEquals(15, result.getTotalElements());
    }

    @Test
    void findAdminOperationLogsRespectsSuppliedSort() {
        UUID actor = UUID.randomUUID();
        Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t2 = Instant.parse("2026-01-02T00:00:00Z");
        Instant t3 = Instant.parse("2026-01-03T00:00:00Z");

        repository.save(AuditLog.create(actor, ActionType.CREATE, ResourceType.MEDICINE, UUID.randomUUID(), null, null, t3));
        repository.save(AuditLog.create(actor, ActionType.CREATE, ResourceType.MEDICINE, UUID.randomUUID(), null, null, t1));
        repository.save(AuditLog.create(actor, ActionType.CREATE, ResourceType.MEDICINE, UUID.randomUUID(), null, null, t2));

        var ascending = repository.findAdminOperationLogs(null, null, null, null,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "createdAt")));
        assertEquals(List.of(t1, t2, t3),
                ascending.getContent().stream().map(AuditLog::getCreatedAt).toList());

        var descending = repository.findAdminOperationLogs(null, null, null, null,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));
        assertEquals(List.of(t3, t2, t1),
                descending.getContent().stream().map(AuditLog::getCreatedAt).toList());
    }

    @Test
    void savedRecordIsReloadedUnchanged() {
        UUID actor = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        String detail = "{\"before\":{\"active\":true},\"after\":{\"active\":false}}";
        Instant at = Instant.parse("2026-01-01T00:00:00Z");

        AuditLog saved = repository.save(AuditLog.create(actor, ActionType.UPDATE, ResourceType.USER,
                resourceId, detail, null, at));
        AuditLog reloaded = repository.findById(saved.getId()).orElseThrow();

        assertEquals(saved.getId(), reloaded.getId());
        assertEquals(actor, reloaded.getUserId());
        assertEquals(ActionType.UPDATE, reloaded.getActionType());
        assertEquals(ResourceType.USER, reloaded.getResourceType());
        assertEquals(resourceId, reloaded.getResourceId());
        assertEquals(detail, reloaded.getDetail());
        assertEquals(at, reloaded.getCreatedAt());
    }
}

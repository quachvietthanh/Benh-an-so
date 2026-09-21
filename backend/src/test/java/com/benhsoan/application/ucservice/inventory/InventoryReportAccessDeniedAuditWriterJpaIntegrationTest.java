package com.benhsoan.application.ucservice.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.persistence.adapterRepository.auditlog.AuditLogRepositoryAdapter;
import com.benhsoan.persistence.entity.auditlog.AuditLogEntity;
import com.benhsoan.persistence.jpaRepository.auditlog.JpaAuditLogRepository;
import com.benhsoan.persistence.mapper.auditlog.AuditLogPersistenceMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({
        InventoryReportAccessDeniedAuditWriter.class,
        AuditLogRepositoryAdapter.class,
        AuditLogPersistenceMapper.class,
        ObjectMapper.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class InventoryReportAccessDeniedAuditWriterJpaIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-21T10:00:00Z");

    @Autowired private InventoryReportAccessDeniedAuditWriter auditWriter;
    @Autowired private JpaAuditLogRepository jpaAuditLogRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    @BeforeEach
    void clearAuditLogs() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jpaAuditLogRepository.deleteAll();
        });
    }

    @Test
    @DisplayName("Audit entry written via REQUIRES_NEW survives rollback of the outer transaction")
    void writeAccessDenied_survivesOuterTransactionRollback() {
        UUID actorId = UUID.randomUUID();
        Set<String> roles = Set.of("DOCTOR");
        String reason = "Chỉ dược sĩ, quản lý phòng khám hoặc quản trị viên mới có quyền truy cập báo cáo xuất nhập tồn kho.";

        TransactionTemplate outerTx = new TransactionTemplate(transactionManager);
        try {
            outerTx.executeWithoutResult(status -> {
                auditWriter.writeAccessDenied(actorId, roles, reason, NOW);
                throw new RuntimeException("Simulated business transaction rollback");
            });
        } catch (RuntimeException ex) {
            assertEquals("Simulated business transaction rollback", ex.getMessage());
        }

        var logs = jpaAuditLogRepository.findAll();
        assertEquals(1, logs.size(), "Denial audit log must be committed and survive the outer transaction rollback");

        AuditLogEntity entry = logs.getFirst();
        assertEquals(actorId, entry.getUserId());
        assertEquals(ActionType.ACCESS_DENIED, entry.getActionType());
        assertEquals(ResourceType.OPERATIONAL_REPORT, entry.getResourceType());
        assertNotNull(entry.getDetail());
        assertTrue(entry.getDetail().contains("INVENTORY_IN_OUT_STOCK_REPORT"));
        assertTrue(entry.getDetail().contains("DOCTOR"));
    }
}

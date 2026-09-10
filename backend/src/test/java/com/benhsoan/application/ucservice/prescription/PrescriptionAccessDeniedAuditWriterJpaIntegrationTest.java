package com.benhsoan.application.ucservice.prescription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
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
        PrescriptionAccessDeniedAuditWriter.class,
        AuditLogRepositoryAdapter.class,
        AuditLogPersistenceMapper.class,
        ObjectMapper.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PrescriptionAccessDeniedAuditWriterJpaIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-10T10:00:00Z");

    @Autowired private PrescriptionAccessDeniedAuditWriter auditWriter;
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
    void writeCancelDenied_survivesOuterTransactionRollback() {
        UUID actorId = UUID.randomUUID();
        UUID prescriptionId = UUID.randomUUID();
        UUID prescribedBy = UUID.randomUUID();

        TransactionTemplate outerTx = new TransactionTemplate(transactionManager);
        try {
            outerTx.executeWithoutResult(status -> {
                auditWriter.writeCancelDenied(
                        actorId,
                        prescriptionId,
                        prescribedBy,
                        NOW,
                        "Attempted to cancel prescription prescribed by another doctor"
                );
                throw new RuntimeException("Simulated business transaction rollback");
            });
        } catch (RuntimeException ex) {
            assertEquals("Simulated business transaction rollback", ex.getMessage());
        }

        var auditLogs = jpaAuditLogRepository.findAll();
        assertEquals(1, auditLogs.size(), "Audit log must survive the outer transaction rollback");

        var log = auditLogs.getFirst();
        assertEquals(actorId, log.getUserId());
        assertEquals(ActionType.ACCESS_DENIED, log.getActionType());
        assertEquals(ResourceType.PRESCRIPTION, log.getResourceType());
        assertEquals(prescriptionId, log.getResourceId());
        assertNotNull(log.getDetail());
        assertTrue(log.getDetail().contains(prescribedBy.toString()));
        assertTrue(log.getDetail().contains("Attempted to cancel prescription prescribed by another doctor"));
    }
}

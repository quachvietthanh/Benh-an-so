package com.benhsoan.application.ucservice.inventory;

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
        MedicationProcurementAuditWriter.class,
        AuditLogRepositoryAdapter.class,
        AuditLogPersistenceMapper.class,
        ObjectMapper.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("Medication Procurement SoD Audit Rollback Survival Integration Test (SEC-01)")
class MedicationProcurementAuditWriterJpaIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-23T10:00:00Z");

    @Autowired
    private MedicationProcurementAuditWriter auditWriter;

    @Autowired
    private JpaAuditLogRepository jpaAuditLogRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void clearAuditLogs() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jpaAuditLogRepository.deleteAll();
        });
    }

    @Test
    @DisplayName("Bản ghi audit ACCESS_DENIED khi vi phạm SoD tự duyệt vẫn tồn tại sau khi transaction nghiệp vụ bị rollback")
    void writeSoDDenied_survivesOuterTransactionRollback() {
        UUID actorId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        String planCode = "DT000001";

        // Giả lập transaction nghiệp vụ bị rollback (ví dụ khi ném SelfProcurementApprovalNotAllowedException)
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                // Ghi audit log qua REQUIRES_NEW
                auditWriter.writeSoDDenied(actorId, planId, planCode, NOW);
                // Ném runtime exception để ép transaction nghiệp vụ bên ngoài rollback
                throw new IllegalStateException("Mô phỏng lỗi nghiệp vụ làm rollback transaction chính");
            });
        } catch (IllegalStateException ignored) {
            // Transaction bên ngoài đã bị rollback
        }

        // Kiểm chứng: Bản ghi kiểm toán vẫn tồn tại trong cơ sở dữ liệu!
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var logs = jpaAuditLogRepository.findAll();
            assertEquals(1, logs.size(), "Bản ghi audit log phải được bảo toàn trong DB dù transaction chính đã rollback");

            AuditLogEntity log = logs.get(0);
            assertEquals(actorId, log.getUserId());
            assertEquals(ActionType.ACCESS_DENIED, log.getActionType());
            assertEquals(ResourceType.MEDICATION_PROCUREMENT_PLAN, log.getResourceType());
            assertEquals(planId, log.getResourceId());
            assertNotNull(log.getDetail());
            assertTrue(log.getDetail().contains("APPROVE_DENIED_SOD"));
            assertTrue(log.getDetail().contains(planCode));
        });
    }

    @Test
    @DisplayName("Bản ghi audit ACCESS_DENIED khi vi phạm SoD tự từ chối vẫn tồn tại sau khi transaction nghiệp vụ bị rollback (P2-02)")
    void writeRejectSoDDenied_survivesOuterTransactionRollback() {
        UUID actorId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        String planCode = "DT000002";

        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                // Ghi audit log từ chối vi phạm SoD qua REQUIRES_NEW
                auditWriter.writeRejectSoDDenied(actorId, planId, planCode, NOW);
                // Giả lập ngoại lệ nghiệp vụ làm rollback
                throw new IllegalStateException("Mô phỏng lỗi nghiệp vụ làm rollback transaction chính khi reject");
            });
        } catch (IllegalStateException ignored) {
            // Transaction chính đã bị rollback
        }

        // Kiểm chứng: Bản ghi kiểm toán REJECT_DENIED_SOD vẫn tồn tại!
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var logs = jpaAuditLogRepository.findAll();
            assertEquals(1, logs.size(), "Bản ghi audit log REJECT_DENIED_SOD phải được bảo toàn trong DB dù transaction chính đã rollback");

            AuditLogEntity log = logs.get(0);
            assertEquals(actorId, log.getUserId());
            assertEquals(ActionType.ACCESS_DENIED, log.getActionType());
            assertEquals(ResourceType.MEDICATION_PROCUREMENT_PLAN, log.getResourceType());
            assertEquals(planId, log.getResourceId());
            assertNotNull(log.getDetail());
            assertTrue(log.getDetail().contains("REJECT_DENIED_SOD"));
            assertTrue(log.getDetail().contains(planCode));
        });
    }
}

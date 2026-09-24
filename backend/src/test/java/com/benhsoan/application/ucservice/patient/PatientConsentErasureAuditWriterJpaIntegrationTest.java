package com.benhsoan.application.ucservice.patient;

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

/**
 * Kiểm thử tích hợp chứng minh tính độc lập giao dịch của PatientConsentErasureAuditWriter (Finding P2-1 / QTN-19).
 * Nhật ký từ chối xóa hồ sơ bệnh án được ghi bằng @Transactional(propagation = REQUIRES_NEW)
 * bắt buộc phải commit thành công và tồn tại trong database ngay cả khi giao dịch nghiệp vụ ngoài bị rollback.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({
        PatientConsentErasureAuditWriter.class,
        AuditLogRepositoryAdapter.class,
        AuditLogPersistenceMapper.class,
        ObjectMapper.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("PatientConsentErasureAuditWriter JPA Integration Test (Rollback Survival - QTN-19, P2-1)")
class PatientConsentErasureAuditWriterJpaIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-24T10:00:00Z");

    @Autowired private PatientConsentErasureAuditWriter auditWriter;
    @Autowired private JpaAuditLogRepository jpaAuditLogRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    @BeforeEach
    void clearAuditLogs() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jpaAuditLogRepository.deleteAll();
        });
    }

    @Test
    @DisplayName("P2-1 & QTN-19: Audit log từ chối xóa dữ liệu ghi bằng REQUIRES_NEW vẫn tồn tại khi transaction nghiệp vụ rollback")
    void writeErasureRefusal_survivesOuterTransactionRollback() {
        UUID actorId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        String patientCode = "PAT-2026-0001";
        int retentionYears = 10;
        String reason = "Yêu cầu xóa toàn bộ dữ liệu kèm ký tự \"ngoặc kép\" và dấu \\ gạch chéo";

        TransactionTemplate outerTx = new TransactionTemplate(transactionManager);
        try {
            outerTx.executeWithoutResult(status -> {
                auditWriter.writeErasureRefusal(
                        actorId,
                        patientId,
                        patientCode,
                        retentionYears,
                        reason,
                        NOW
                );
                throw new RuntimeException("Giả lập lỗi rollback transaction nghiệp vụ ngoài");
            });
        } catch (RuntimeException ex) {
            assertEquals("Giả lập lỗi rollback transaction nghiệp vụ ngoài", ex.getMessage());
        }

        var auditLogs = jpaAuditLogRepository.findAll();
        assertEquals(1, auditLogs.size(), "Audit log từ chối xóa dữ liệu bắt buộc phải sống sót sau rollback transaction ngoài");

        var log = auditLogs.getFirst();
        assertEquals(actorId, log.getUserId());
        assertEquals(ActionType.UPDATE, log.getActionType());
        assertEquals(ResourceType.PATIENT, log.getResourceType());
        assertEquals(patientId, log.getResourceId());
        assertNotNull(log.getDetail());
        assertTrue(log.getDetail().contains("DATA_ERASURE_REQUEST"));
        assertTrue(log.getDetail().contains("PAT-2026-0001"));
        assertTrue(log.getDetail().contains("10"));
        assertTrue(log.getDetail().contains("QTN-19"));
        assertTrue(log.getDetail().contains("ngoặc kép"));
    }
}

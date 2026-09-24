package com.benhsoan.application.ucservice.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("PatientConsentErasureAuditWriter Unit Tests (QTN-19)")
class PatientConsentErasureAuditWriterTest {

    private static final Instant NOW = Instant.parse("2026-09-23T10:00:00Z");

    @Mock
    private AuditLogRepository auditLogRepository;

    private PatientConsentErasureAuditWriter auditWriter;

    @BeforeEach
    void setUp() {
        auditWriter = new PatientConsentErasureAuditWriter(auditLogRepository, new ObjectMapper());
    }

    @Test
    @DisplayName("QTN-19: writeErasureRefusal ghi nhận đầy đủ thông tin từ chối xóa hồ sơ bệnh án")
    void writeErasureRefusal_savesAuditLogCorrectly() {
        UUID actorId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        String patientCode = "PAT-2026-0001";
        int retentionYears = 10;
        String reason = "Bệnh nhân yêu cầu xóa dữ liệu";

        auditWriter.writeErasureRefusal(actorId, patientId, patientCode, retentionYears, reason, NOW);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertNotNull(savedLog);
        assertEquals(actorId, savedLog.getUserId());
        assertEquals(ActionType.UPDATE, savedLog.getActionType());
        assertEquals(ResourceType.PATIENT, savedLog.getResourceType());
        assertEquals(patientId, savedLog.getResourceId());
        assertEquals(NOW, savedLog.getCreatedAt());

        String detail = savedLog.getDetail();
        assertTrue(detail.contains("DATA_ERASURE_REQUEST"));
        assertTrue(detail.contains("PAT-2026-0001"));
        assertTrue(detail.contains("10"));
        assertTrue(detail.contains("Bệnh nhân yêu cầu xóa dữ liệu"));
        assertTrue(detail.contains("QTN-19"));
    }

    @Test
    @DisplayName("QTN-19: writeErasureRefusal được cấu hình Propagation.REQUIRES_NEW để độc lập giao dịch khi rollback")
    void writeErasureRefusal_hasRequiresNewTransactionPropagation() throws NoSuchMethodException {
        Method method = PatientConsentErasureAuditWriter.class.getMethod(
                "writeErasureRefusal",
                UUID.class,
                UUID.class,
                String.class,
                int.class,
                String.class,
                Instant.class
        );

        Transactional transactional = method.getAnnotation(Transactional.class);
        assertNotNull(transactional, "Method writeErasureRefusal bắt buộc phải có annotation @Transactional");
        assertEquals(Propagation.REQUIRES_NEW, transactional.propagation(),
                "Propagation phải là REQUIRES_NEW để audit log không bị mất khi giao dịch chính rollback theo QTN-19");
    }

    @Test
    @DisplayName("P3-4: writeErasureRefusal serialize an toàn lý do chứa ký tự đặc biệt (ngoặc kép, gạch chéo ngược)")
    void writeErasureRefusal_escapesSpecialCharactersInReasonSafely() throws Exception {
        UUID actorId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        String specialReason = "Bệnh nhân yêu cầu xóa: \"toàn bộ\" thông tin\\dữ liệu\nvà không lưu lại.";

        auditWriter.writeErasureRefusal(actorId, patientId, "PAT-002", 10, specialReason, NOW);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertNotNull(savedLog);
        String detail = savedLog.getDetail();

        // Kiểm tra tính hợp lệ của chuỗi JSON bằng ObjectMapper parser thật
        com.fasterxml.jackson.databind.JsonNode rootNode = new ObjectMapper().readTree(detail);
        assertEquals("DATA_ERASURE_REQUEST", rootNode.get("action").asText());
        assertEquals("PAT-002", rootNode.get("patientCode").asText());
        assertEquals(10, rootNode.get("retentionYears").asInt());
        assertEquals(specialReason, rootNode.get("reason").asText());
    }
}

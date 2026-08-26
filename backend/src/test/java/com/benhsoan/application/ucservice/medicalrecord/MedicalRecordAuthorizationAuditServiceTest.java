package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;

@ExtendWith(MockitoExtension.class)
class MedicalRecordAuthorizationAuditServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @InjectMocks private MedicalRecordAuthorizationAuditService service;

    @Test
    void recordsDeniedDiagnosisWriteAgainstMedicalRecord() {
        UUID actorId = UUID.randomUUID();
        UUID medicalRecordId = UUID.randomUUID();

        service.recordDiagnosisWriteDenied(actorId, medicalRecordId);

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLog audit = auditCaptor.getValue();
        assertEquals(actorId, audit.getUserId());
        assertEquals(ActionType.ACCESS_DENIED, audit.getActionType());
        assertEquals(ResourceType.MEDICAL_RECORD, audit.getResourceType());
        assertEquals(medicalRecordId, audit.getResourceId());
    }
}

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

    @Test
    void recordsDeniedVisitTemplateAccessAgainstVisit() {
        UUID actorId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        String detail = "User lacks read permission on visit template options";

        service.recordVisitTemplateAccessDenied(actorId, visitId, detail);

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLog audit = auditCaptor.getValue();
        assertEquals(actorId, audit.getUserId());
        assertEquals(ActionType.ACCESS_DENIED, audit.getActionType());
        assertEquals(ResourceType.VISIT, audit.getResourceType());
        assertEquals(visitId, audit.getResourceId());
        assertEquals(detail, audit.getDetail());
    }

    @Test
    void recordsTemplateConfigurationFailureAgainstGivenResource() {
        UUID actorId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        String detail = "Default template not configured for specialty GENERAL";

        service.recordTemplateConfigurationFailure(actorId, resourceId, ResourceType.VISIT, detail);

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLog audit = auditCaptor.getValue();
        assertEquals(actorId, audit.getUserId());
        assertEquals(ActionType.ACCESS_DENIED, audit.getActionType());
        assertEquals(ResourceType.VISIT, audit.getResourceType());
        assertEquals(resourceId, audit.getResourceId());
        assertEquals(detail, audit.getDetail());
    }
}

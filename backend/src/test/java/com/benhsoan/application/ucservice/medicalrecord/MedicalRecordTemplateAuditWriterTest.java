package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.medicalrecord.MedicalRecordTemplate;
import com.benhsoan.domain.medicalrecord.MedicalRecordTemplateVersion.SectionDefinition;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordFieldCode;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;

@ExtendWith(MockitoExtension.class)
class MedicalRecordTemplateAuditWriterTest {

    @Mock private AuditLogRepository auditLogRepository;

    @Test
    void writesTemplateIdAndVersionForSuccessfulUpdate() {
        UUID actorId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-26T00:00:00Z");
        MedicalRecordTemplate template = MedicalRecordTemplate.create(UUID.randomUUID(), "General", false,
                List.of(new SectionDefinition(MedicalRecordFieldCode.CHIEF_COMPLAINT, "Chief complaint", true, 1)),
                actorId, now);
        template.update("General follow-up",
                List.of(new SectionDefinition(MedicalRecordFieldCode.CHIEF_COMPLAINT, "Chief complaint", true, 1)),
                null, actorId, now.plusSeconds(1));

        new MedicalRecordTemplateAuditWriter(auditLogRepository).writeUpdated(actorId, template, now.plusSeconds(1));

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLog audit = auditCaptor.getValue();
        assertEquals(ActionType.UPDATE, audit.getActionType());
        assertEquals(ResourceType.MEDICAL_RECORD_TEMPLATE, audit.getResourceType());
        assertEquals(template.getId(), audit.getResourceId());
        assertTrue(audit.getDetail().contains("version=2"));
    }
}

package com.benhsoan.application.ucservice.medicalrecord;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.medicalrecord.MedicalRecordTemplate;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MedicalRecordTemplateAuditWriter {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void writeCreated(UUID actorId, MedicalRecordTemplate template, Instant now) {
        write(actorId, ActionType.CREATE, template, "Medical record template created", now);
    }

    @Transactional
    public void writeUpdated(UUID actorId, MedicalRecordTemplate template, Instant now) {
        write(actorId, ActionType.UPDATE, template, "Medical record template version created", now);
    }

    @Transactional
    public void writeDefaultSet(UUID actorId, MedicalRecordTemplate template, Instant now) {
        write(actorId, ActionType.UPDATE, template, "Medical record template set as default", now);
    }

    @Transactional
    public void writeStatusChanged(UUID actorId, MedicalRecordTemplate template, Instant now) {
        write(actorId, template.isActive() ? ActionType.ACTIVATE : ActionType.DEACTIVATE, template,
                "Medical record template status changed", now);
    }

    private void write(UUID actorId, ActionType action, MedicalRecordTemplate template, String event, Instant now) {
        auditLogRepository.save(AuditLog.create(actorId, action, ResourceType.MEDICAL_RECORD_TEMPLATE, template.getId(),
                event + "; templateId=" + template.getId() + "; version=" + template.getCurrentVersionNo() + ".",
                null, now));
    }
}

package com.benhsoan.application.ucservice.prescription;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionItem;
import com.benhsoan.domain.prescription.PrescriptionTemplate;
import com.benhsoan.domain.prescription.PrescriptionTemplateItem;
import com.benhsoan.domain.prescription.exception.PrescriptionNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.prescription.SavePrescriptionTemplateCommand;
import com.benhsoan.port.dto.result.PrescriptionTemplateResult;
import com.benhsoan.port.inbound.prescription.SavePrescriptionTemplateUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.DiagnosisCatalogRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionTemplateRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/** NCL-05-CN-008 TC-01: saves a completed prescription as a doctor-scoped template. */
@Service
@RequiredArgsConstructor
@Transactional
public class SavePrescriptionTemplateService implements SavePrescriptionTemplateUseCase {

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionTemplateRepository templateRepository;
    private final DiagnosisCatalogRepository diagnosisCatalogRepository;
    private final PrescriptionTemplateResultMapper resultMapper;
    private final CurrentUserPort currentUserPort;
    private final AuditLogRepository auditLogRepository;
    private final ClockPort clockPort;
    private final ObjectMapper objectMapper;

    @Override
    public PrescriptionTemplateResult save(SavePrescriptionTemplateCommand command) {
        requireCommand(command);
        authorizeDoctor();
        UUID currentUserId = currentUserPort.getCurrentUserId();
        Instant now = clockPort.now();

        Prescription prescription = prescriptionRepository.findById(command.prescriptionId())
                .orElseThrow(() -> new PrescriptionNotFoundException(command.prescriptionId()));

        if (!prescription.isPendingDispense()) {
            throw new ValidationException(
                    "Only a completed (pending-dispense) prescription can be saved as a template.");
        }
        if (!Objects.equals(prescription.getPrescribedBy(), currentUserId)) {
            throw new AccessDeniedException(
                    "Only the doctor who prescribed can save this prescription as a template.");
        }

        DiagnosisCatalog diagnosis = diagnosisCatalogRepository.findByCode(command.diagnosisCode())
                .orElseThrow(() -> new ValidationException(
                        "Diagnosis code not found: " + command.diagnosisCode()));

        UUID templateId = UUID.randomUUID();
        PrescriptionTemplate template = PrescriptionTemplate.create(
                templateId,
                diagnosis.getId(),
                currentUserId,
                now,
                buildItems(templateId, prescription)
        );

        PrescriptionTemplate saved = templateRepository.save(template);
        auditLogRepository.save(AuditLog.create(
                currentUserId,
                ActionType.CREATE,
                ResourceType.PRESCRIPTION_TEMPLATE,
                saved.getId(),
                auditDetail(saved, diagnosis, prescription.getId()),
                null,
                now
        ));

        return resultMapper.toResult(saved);
    }

    private List<PrescriptionTemplateItem> buildItems(UUID templateId, Prescription prescription) {
        List<PrescriptionTemplateItem> items = new ArrayList<>();
        int sortOrder = 0;
        for (PrescriptionItem item : prescription.getItems()) {
            items.add(PrescriptionTemplateItem.create(
                    templateId,
                    item.getMedicineId(),
                    item.getDosage(),
                    item.getFrequency(),
                    item.getRoute(),
                    item.getDurationDays(),
                    item.getQuantity(),
                    item.getInstructions(),
                    sortOrder++
            ));
        }
        return items;
    }

    private String auditDetail(PrescriptionTemplate template, DiagnosisCatalog diagnosis, UUID source) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("diagnosisCode", diagnosis.getCode());
        detail.put("diagnosisCatalogId", diagnosis.getId().toString());
        detail.put("sourcePrescriptionId", source.toString());
        detail.put("itemCount", template.getItems().size());
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize prescription template audit detail.", exception);
        }
    }

    private void authorizeDoctor() {
        if (!currentUserPort.hasRole("DOCTOR")) {
            throw new AccessDeniedException("Only doctors are allowed to save prescription templates.");
        }
    }

    private void requireCommand(SavePrescriptionTemplateCommand command) {
        if (command == null || command.prescriptionId() == null) {
            throw new ValidationException("Prescription id is required.");
        }
        if (command.diagnosisCode() == null || command.diagnosisCode().isBlank()) {
            throw new ValidationException("Diagnosis code is required.");
        }
    }
}

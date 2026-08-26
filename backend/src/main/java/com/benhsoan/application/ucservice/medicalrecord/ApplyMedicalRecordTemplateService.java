package com.benhsoan.application.ucservice.medicalrecord;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.MedicalRecordTemplate;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordInvalidVisitException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateChangeWithContentException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateDefaultNotConfiguredException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateInactiveException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateNotFoundException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateSpecialtyMismatchException;
import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.medicalrecord.ApplyMedicalRecordTemplateCommand;
import com.benhsoan.port.dto.result.MedicalRecordResult;
import com.benhsoan.port.inbound.medicalrecord.ApplyMedicalRecordTemplateUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordTemplateRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ApplyMedicalRecordTemplateService implements ApplyMedicalRecordTemplateUseCase {

    private final MedicalRecordRepository medicalRecordRepository;
    private final VisitRepository visitRepository;
    private final MedicalRecordTemplateRepository templateRepository;
    private final MedicalRecordAuthorizationService authorizationService;
    private final MedicalRecordAccessAuditService accessAuditService;
    private final MedicalRecordTemplateApplicationMapper templateMapper;
    private final MedicalRecordResultMapper resultMapper;
    private final ClockPort clockPort;

    @Override
    public MedicalRecordResult apply(UUID medicalRecordId, ApplyMedicalRecordTemplateCommand command) {
        UUID actorId = authorizationService.requireTemplateWriteAccess(medicalRecordId);
        MedicalRecord record = medicalRecordRepository.findByIdForUpdate(medicalRecordId)
                .orElseThrow(() -> new MedicalRecordNotFoundException(medicalRecordId));
        record.ensureEditable();
        Visit visit = visitRepository.findById(record.getVisitId())
                .orElseThrow(() -> new VisitNotFoundException(record.getVisitId()));
        authorizationService.requireTemplateVisitAccess(actorId, visit.getDoctorId(), record.getId());
        if (!visit.isActive()) throw new MedicalRecordInvalidVisitException(visit.getId());
        if (command == null || command.templateId() == null) {
            throw new com.benhsoan.domain.shared.exception.ValidationException("Template id is required.");
        }
        MedicalRecordTemplate selected = templateRepository.findByIdForUpdate(command.templateId())
                .orElseThrow(() -> new MedicalRecordTemplateNotFoundException(command.templateId()));
        if (!selected.isActive()) throw new MedicalRecordTemplateInactiveException();
        boolean fallback = validateTemplateForVisit(selected, visit);
        if (record.hasClinicalContent()) {
            throw new MedicalRecordTemplateChangeWithContentException();
        }
        Instant now = clockPort.now();
        record.applyTemplateVersion(selected.getCurrentVersion().getId(), actorId, now);
        MedicalRecord saved = medicalRecordRepository.save(record);
        String detail = "Template applied: template=" + selected.getId() + ", version="
                + selected.getCurrentVersion().getId() + ", specialty=" + selected.getSpecialtyId()
                + ", fallback=" + fallback;
        accessAuditService.recordRecordAccessInCurrentTransaction(visit.getPatientId(), visit.getId(), saved.getId(),
                actorId, MedicalRecordAccessAction.TEMPLATE_APPLY, detail, now);
        return resultMapper.toResult(saved, templateMapper.resolveApplied(saved, visit));
    }

    private boolean validateTemplateForVisit(MedicalRecordTemplate selected, Visit visit) {
        List<MedicalRecordTemplate> applicable = templateRepository.findBySpecialtyIdAndActive(visit.getSpecialtyId(), true);
        if (!applicable.isEmpty()) {
            if (!selected.getSpecialtyId().equals(visit.getSpecialtyId())) throw new MedicalRecordTemplateSpecialtyMismatchException();
            return false;
        }
        List<MedicalRecordTemplate> general = templateRepository.findBySpecialtyIdAndActive(Specialty.GENERAL_ID, true);
        List<MedicalRecordTemplate> defaults = general.stream().filter(MedicalRecordTemplate::isDefaultTemplate).toList();
        if (defaults.size() != 1) throw new MedicalRecordTemplateDefaultNotConfiguredException();
        if (!selected.getId().equals(defaults.getFirst().getId())) throw new MedicalRecordTemplateSpecialtyMismatchException();
        return true;
    }
}

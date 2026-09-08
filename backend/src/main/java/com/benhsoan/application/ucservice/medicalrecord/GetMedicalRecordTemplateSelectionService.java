package com.benhsoan.application.ucservice.medicalrecord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.MedicalRecordTemplate;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateDefaultNotConfiguredException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.result.MedicalRecordTemplateSelectionResult;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordTemplateSelectionUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordTemplateRepository;
import com.benhsoan.port.outbound.repository.specialty.SpecialtyRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMedicalRecordTemplateSelectionService implements GetMedicalRecordTemplateSelectionUseCase {

    private final MedicalRecordRepository medicalRecordRepository;
    private final VisitRepository visitRepository;
    private final SpecialtyRepository specialtyRepository;
    private final MedicalRecordTemplateRepository templateRepository;
    private final MedicalRecordAuthorizationService authorizationService;
    private final MedicalRecordAuthorizationAuditService authorizationAuditService;
    private final MedicalRecordAccessAuditService accessAuditService;
    private final MedicalRecordTemplateApplicationMapper templateMapper;
    private final ClockPort clockPort;

    @Override
    public MedicalRecordTemplateSelectionResult getForMedicalRecord(UUID medicalRecordId) {
        MedicalRecord record = medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> new MedicalRecordNotFoundException(medicalRecordId));
        Visit visit = visitRepository.findById(record.getVisitId())
                .orElseThrow(() -> new VisitNotFoundException(record.getVisitId()));
        return select(visit, record);
    }

    @Override
    public MedicalRecordTemplateSelectionResult getForVisit(UUID visitId) {
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new VisitNotFoundException(visitId));
        Optional<MedicalRecord> record = medicalRecordRepository.findByVisitId(visitId);
        return select(visit, record.orElse(null));
    }

    private MedicalRecordTemplateSelectionResult select(Visit visit, MedicalRecord record) {
        UUID actorId;
        if (record != null) {
            actorId = authorizationService.requireTemplateReadAccess(record.getId());
            authorizationService.requireTemplateVisitAccess(actorId, visit.getDoctorId(), record.getId());
        } else {
            actorId = authorizationService.requireVisitTemplateReadAccess(visit.getId());
            authorizationService.requireVisitTemplateVisitAccess(actorId, visit.getDoctorId(), visit.getId());
        }

        var visitSpecialty = specialtyRepository.findById(visit.getSpecialtyId())
                .orElseThrow(() -> new com.benhsoan.domain.specialty.exception.SpecialtyNotFoundException(visit.getSpecialtyId()));
        List<MedicalRecordTemplate> applicable = templateRepository.findBySpecialtyIdAndActive(visit.getSpecialtyId(), true);
        boolean fallback = applicable.isEmpty();
        List<MedicalRecordTemplate> available = fallback
                ? templateRepository.findBySpecialtyIdAndActive(com.benhsoan.domain.specialty.Specialty.GENERAL_ID, true)
                : applicable;

        MedicalRecordTemplate effective;
        if (fallback) {
            List<MedicalRecordTemplate> defaults = available.stream().filter(MedicalRecordTemplate::isDefaultTemplate).toList();
            if (defaults.size() != 1) {
                UUID resId = record != null ? record.getId() : visit.getId();
                ResourceType resType = record != null ? ResourceType.MEDICAL_RECORD : ResourceType.VISIT;
                authorizationAuditService.recordTemplateConfigurationFailure(actorId, resId, resType,
                        "Default template not configured for specialty GENERAL in fallback mode");
                throw new MedicalRecordTemplateDefaultNotConfiguredException();
            }
            effective = defaults.getFirst();
        } else {
            List<MedicalRecordTemplate> defaults = available.stream().filter(MedicalRecordTemplate::isDefaultTemplate).toList();
            if (!defaults.isEmpty()) {
                effective = defaults.getFirst();
            } else {
                effective = available.getFirst();
            }
        }

        List<MedicalRecordTemplate> optionsToReturn = fallback ? List.of(effective) : available;

        if (record != null) {
            accessAuditService.recordRecordView(visit.getPatientId(), visit.getId(), record.getId(), actorId, clockPort.now());
        }
        return new MedicalRecordTemplateSelectionResult(
                record != null ? record.getId() : null,
                visit.getId(),
                templateMapper.toSpecialty(visitSpecialty),
                optionsToReturn.stream().map(templateMapper::toOption).toList(),
                templateMapper.toOption(effective),
                fallback
        );
    }
}

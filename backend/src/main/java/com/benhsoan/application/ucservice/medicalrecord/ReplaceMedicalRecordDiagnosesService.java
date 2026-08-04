package com.benhsoan.application.ucservice.medicalrecord;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.MedicalRecordDiagnosis;
import com.benhsoan.domain.medicalrecord.enums.DiagnosisType;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordInvalidVisitException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.medicalrecord.ReplaceMedicalRecordDiagnosesCommand;
import com.benhsoan.port.dto.result.MedicalRecordDiagnosisResult;
import com.benhsoan.port.inbound.medicalrecord.ReplaceMedicalRecordDiagnosesUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.DiagnosisCatalogRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordDiagnosisRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ReplaceMedicalRecordDiagnosesService implements ReplaceMedicalRecordDiagnosesUseCase {

    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicalRecordDiagnosisRepository medicalRecordDiagnosisRepository;
    private final DiagnosisCatalogRepository diagnosisCatalogRepository;
    private final VisitRepository visitRepository;
    private final MedicalRecordAuthorizationService authorizationService;
    private final MedicalRecordAccessAuditService accessAuditService;
    private final MedicalRecordDiagnosisResultMapper resultMapper;
    private final ClockPort clockPort;

    @Override
    public List<MedicalRecordDiagnosisResult> replace(
            UUID medicalRecordId,
            ReplaceMedicalRecordDiagnosesCommand command
    ) {
        UUID actorId = authorizationService.requireWriteAccess();
        MedicalRecord record = medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> new MedicalRecordNotFoundException(medicalRecordId));
        record.ensureEditable();
        var visit = visitRepository.findById(record.getVisitId())
                .orElseThrow(() -> new VisitNotFoundException(record.getVisitId()));
        if (!visit.isActive()) {
            throw new MedicalRecordInvalidVisitException(visit.getId());
        }

        Instant now = clockPort.now();
        List<MedicalRecordDiagnosis> diagnoses = buildDiagnoses(record.getId(), command, actorId, now);
        List<MedicalRecordDiagnosis> saved = medicalRecordDiagnosisRepository.replaceForMedicalRecord(record.getId(), diagnoses);
        accessAuditService.recordRecordAccess(visit.getPatientId(), visit.getId(), record.getId(), actorId,
                MedicalRecordAccessAction.UPDATE, "Medical record diagnoses replaced", now);
        return saved.stream().map(resultMapper::toResult).toList();
    }

    private List<MedicalRecordDiagnosis> buildDiagnoses(
            UUID medicalRecordId,
            ReplaceMedicalRecordDiagnosesCommand command,
            UUID actorId,
            Instant diagnosedAt
    ) {
        if (command == null || command.primaryDiagnosis() == null) {
            throw new ValidationException("Primary diagnosis is required.");
        }
        List<ReplaceMedicalRecordDiagnosesCommand.DiagnosisCommand> requested = new ArrayList<>();
        requested.add(command.primaryDiagnosis());
        if (command.secondaryDiagnoses() != null) {
            requested.addAll(command.secondaryDiagnoses());
        }
        validateNoDuplicates(requested);

        List<MedicalRecordDiagnosis> diagnoses = new ArrayList<>();
        diagnoses.add(toDiagnosis(medicalRecordId, command.primaryDiagnosis(), DiagnosisType.PRIMARY, actorId, diagnosedAt));
        for (ReplaceMedicalRecordDiagnosesCommand.DiagnosisCommand secondary : requested.subList(1, requested.size())) {
            diagnoses.add(toDiagnosis(medicalRecordId, secondary, DiagnosisType.SECONDARY, actorId, diagnosedAt));
        }
        return diagnoses;
    }

    private MedicalRecordDiagnosis toDiagnosis(
            UUID medicalRecordId,
            ReplaceMedicalRecordDiagnosesCommand.DiagnosisCommand command,
            DiagnosisType type,
            UUID actorId,
            Instant diagnosedAt
    ) {
        if (command == null || command.diagnosisCatalogId() == null) {
            throw new ValidationException("Diagnosis catalog is required.");
        }
        DiagnosisCatalog catalog = diagnosisCatalogRepository.findById(command.diagnosisCatalogId())
                .filter(DiagnosisCatalog::isActive)
                .orElseThrow(() -> new ValidationException("Diagnosis catalog is unavailable."));
        if (!normalize(command.code()).equals(normalize(catalog.getCode()))
                || !normalize(command.name()).equals(normalize(catalog.getName()))) {
            throw new ValidationException("Diagnosis code and name must match the diagnosis catalog.");
        }
        return MedicalRecordDiagnosis.create(medicalRecordId, catalog.getId(), catalog.getCode(), catalog.getName(),
                type, command.note(), actorId, diagnosedAt);
    }

    private void validateNoDuplicates(List<ReplaceMedicalRecordDiagnosesCommand.DiagnosisCommand> diagnoses) {
        Set<UUID> catalogIds = new HashSet<>();
        Set<String> codes = new HashSet<>();
        for (ReplaceMedicalRecordDiagnosesCommand.DiagnosisCommand diagnosis : diagnoses) {
            if (diagnosis == null || diagnosis.diagnosisCatalogId() == null || normalize(diagnosis.code()).isEmpty()) {
                throw new ValidationException("Diagnosis catalog and code are required.");
            }
            if (!catalogIds.add(diagnosis.diagnosisCatalogId()) || !codes.add(normalize(diagnosis.code()))) {
                throw new ValidationException("A diagnosis cannot be repeated in the same medical record.");
            }
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}

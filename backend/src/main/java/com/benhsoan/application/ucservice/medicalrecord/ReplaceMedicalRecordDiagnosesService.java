package com.benhsoan.application.ucservice.medicalrecord;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
        UUID actorId = authorizationService.requireDiagnosisWriteAccess(medicalRecordId);
        MedicalRecord record = medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> new MedicalRecordNotFoundException(medicalRecordId));
        record.ensureEditable();
        var visit = visitRepository.findById(record.getVisitId())
                .orElseThrow(() -> new VisitNotFoundException(record.getVisitId()));
        authorizationService.requireDiagnosisVisitWriteAccess(actorId, visit.getDoctorId(), record.getId());
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
        List<ReplaceMedicalRecordDiagnosesCommand.SecondaryDiagnosisCommand> secondaryDiagnoses = command.secondaryDiagnoses() == null
                ? List.of()
                : command.secondaryDiagnoses();
        validateNoDuplicateCatalogIds(command.primaryDiagnosis(), secondaryDiagnoses);

        List<MedicalRecordDiagnosis> diagnoses = new ArrayList<>();
        diagnoses.add(toPrimaryDiagnosis(medicalRecordId, command.primaryDiagnosis(), actorId, diagnosedAt));
        for (ReplaceMedicalRecordDiagnosesCommand.SecondaryDiagnosisCommand secondary : secondaryDiagnoses) {
            diagnoses.add(toSecondaryDiagnosis(medicalRecordId, secondary, actorId, diagnosedAt));
        }
        return diagnoses;
    }

    private MedicalRecordDiagnosis toPrimaryDiagnosis(
            UUID medicalRecordId,
            ReplaceMedicalRecordDiagnosesCommand.PrimaryDiagnosisCommand command,
            UUID actorId,
            Instant diagnosedAt
    ) {
        if (command == null || command.diagnosisCatalogId() == null) {
            throw new ValidationException("Diagnosis catalog is required.");
        }
        return toCatalogDiagnosis(medicalRecordId, command.diagnosisCatalogId(), DiagnosisType.PRIMARY, command.note(), actorId, diagnosedAt);
    }

    private MedicalRecordDiagnosis toSecondaryDiagnosis(
            UUID medicalRecordId,
            ReplaceMedicalRecordDiagnosesCommand.SecondaryDiagnosisCommand command,
            UUID actorId,
            Instant diagnosedAt
    ) {
        if (command == null) {
            throw new ValidationException("Secondary diagnosis is required.");
        }
        if (command.diagnosisCatalogId() != null) {
            if (hasText(command.name())) {
                throw new ValidationException("Secondary diagnosis must use either a catalog entry or free-text name.");
            }
            return toCatalogDiagnosis(medicalRecordId, command.diagnosisCatalogId(), DiagnosisType.SECONDARY,
                    command.note(), actorId, diagnosedAt);
        }
        if (!hasText(command.name())) {
            throw new ValidationException("Secondary diagnosis name is required when no catalog entry is selected.");
        }
        return MedicalRecordDiagnosis.create(medicalRecordId, null, null, command.name().trim(), DiagnosisType.SECONDARY,
                command.note(), actorId, diagnosedAt);
    }

    private MedicalRecordDiagnosis toCatalogDiagnosis(
            UUID medicalRecordId,
            UUID diagnosisCatalogId,
            DiagnosisType type,
            String note,
            UUID actorId,
            Instant diagnosedAt
    ) {
        DiagnosisCatalog catalog = diagnosisCatalogRepository.findById(diagnosisCatalogId)
                .filter(DiagnosisCatalog::isActive)
                .orElseThrow(() -> new ValidationException("Diagnosis catalog is unavailable."));
        return MedicalRecordDiagnosis.create(medicalRecordId, catalog.getId(), catalog.getCode(), catalog.getName(),
                type, note, actorId, diagnosedAt);
    }

    private void validateNoDuplicateCatalogIds(
            ReplaceMedicalRecordDiagnosesCommand.PrimaryDiagnosisCommand primary,
            List<ReplaceMedicalRecordDiagnosesCommand.SecondaryDiagnosisCommand> secondaryDiagnoses
    ) {
        if (primary.diagnosisCatalogId() == null) {
            throw new ValidationException("Diagnosis catalog is required.");
        }
        Set<UUID> catalogIds = new HashSet<>();
        catalogIds.add(primary.diagnosisCatalogId());
        for (ReplaceMedicalRecordDiagnosesCommand.SecondaryDiagnosisCommand secondary : secondaryDiagnoses) {
            if (secondary != null && secondary.diagnosisCatalogId() != null && !catalogIds.add(secondary.diagnosisCatalogId())) {
                throw new ValidationException("A diagnosis cannot be repeated in the same medical record.");
            }
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

package com.benhsoan.application.ucservice.medicalrecord;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import com.benhsoan.domain.medicalrecord.MedicalRecordDiagnosis;
import com.benhsoan.domain.medicalrecord.enums.DiagnosisType;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.port.dto.command.medicalrecord.RecordDiagnosisCommand;
import com.benhsoan.port.dto.result.ExaminationDiagnosisResult;
import com.benhsoan.port.dto.result.ExaminationDiagnosisResult.SecondaryDiagnosis;
import com.benhsoan.port.inbound.medicalrecord.GetExaminationDiagnosisUseCase;
import com.benhsoan.port.inbound.medicalrecord.RecordDiagnosisUseCase;
import com.benhsoan.port.outbound.repository.crudRepository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ExaminationDiagnosisService
        implements RecordDiagnosisUseCase, GetExaminationDiagnosisUseCase {

    private final VisitRepository visitRepository;
    private final CurrentUserPort currentUserPort;

    @Override
    public ExaminationDiagnosisResult recordDiagnosis(UUID examinationId, RecordDiagnosisCommand command) {
        Visit visit = visitRepository.findById(examinationId)
                .orElseThrow(() -> new ValidationException("Examination not found: " + examinationId));

        // QTN-07: Lock diagnosis after encounter completed
        if (visit.getStatus() == VisitStatus.COMPLETED || visit.getStatus() == VisitStatus.CANCELLED) {
            throw new ValidationException("Cannot record diagnosis on a completed or cancelled examination.");
        }

        UUID doctorId = currentUserPort.getCurrentUserId();
        Instant now = Instant.now();

        // Use the command's diagnosis info to record
        MedicalRecordDiagnosis primaryDiagnosis = MedicalRecordDiagnosis.create(
                examinationId,
                command.diagnosisCatalogId(),
                command.primaryIcdCode(),
                command.primaryIcdName(),
                DiagnosisType.PRIMARY,
                command.clinicalNotes(),
                doctorId,
                now
        );

        // For now, build result from command data (persistence will be enhanced later)
        List<SecondaryDiagnosis> secondary = List.of();
        if (command.secondaryIcdCodes() != null) {
            secondary = command.secondaryIcdCodes().stream()
                    .map(s -> new SecondaryDiagnosis(null, s.code(), s.name()))
                    .toList();
        }

        return new ExaminationDiagnosisResult(
                primaryDiagnosis.getId(),
                examinationId,
                doctorId,
                command.primaryIcdCode(),
                command.primaryIcdName(),
                secondary,
                command.clinicalNotes(),
                now,
                List.of()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ExaminationDiagnosisResult getDiagnosis(UUID examinationId) {
        Visit visit = visitRepository.findById(examinationId)
                .orElseThrow(() -> new ValidationException("Examination not found: " + examinationId));

        // Return a simplified result for now
        return new ExaminationDiagnosisResult(
                null,
                examinationId,
                visit.getDoctorId(),
                null,
                null,
                List.of(),
                null,
                null,
                List.of()
        );
    }
}

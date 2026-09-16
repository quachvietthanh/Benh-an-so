package com.benhsoan.application.ucservice.medicalrecord;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordInvalidVisitException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.medicalrecord.UpdateInstructionsAndTreatmentPlanCommand;
import com.benhsoan.port.dto.result.MedicalRecordResult;
import com.benhsoan.port.inbound.medicalrecord.UpdateInstructionsAndTreatmentPlanUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateInstructionsAndTreatmentPlanService implements UpdateInstructionsAndTreatmentPlanUseCase {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final MedicalRecordRepository medicalRecordRepository;
    private final VisitRepository visitRepository;
    private final MedicalRecordAuthorizationService authorizationService;
    private final MedicalRecordAccessAuditService accessAuditService;
    private final MedicalRecordResultMapper resultMapper;
    private final ClockPort clockPort;

    @Override
    public MedicalRecordResult updateInstructionsAndTreatmentPlan(UUID medicalRecordId, UpdateInstructionsAndTreatmentPlanCommand command) {
        UUID userId = authorizationService.requireContentWriteAccess(medicalRecordId);
        MedicalRecord record = medicalRecordRepository.findByIdForUpdate(medicalRecordId)
                .orElseThrow(() -> new MedicalRecordNotFoundException(medicalRecordId));
        record.ensureEditable();
        Visit visit = visitRepository.findById(record.getVisitId())
                .orElseThrow(() -> new VisitNotFoundException(record.getVisitId()));
        authorizationService.requireContentVisitWriteAccess(userId, visit.getDoctorId(), record.getId());
        if (!visit.isActive()) {
            throw new MedicalRecordInvalidVisitException(visit.getId());
        }

        LocalDate visitDate = visit.getVisitAt() != null ? visit.getVisitAt().atZone(CLINIC_ZONE).toLocalDate() : null;
        Instant now = clockPort.now();

        record.updateInstructionsAndTreatmentPlan(
                command.treatmentPlan(),
                command.doctorInstructions(),
                command.revisitDate(),
                visitDate,
                userId,
                now
        );

        MedicalRecord saved = medicalRecordRepository.save(record);
        accessAuditService.recordRecordAccess(visit.getPatientId(), visit.getId(), saved.getId(), userId,
                MedicalRecordAccessAction.UPDATE, "Doctor instructions and treatment plan updated", now);
        return resultMapper.toResult(saved);
    }
}

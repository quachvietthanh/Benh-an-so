package com.benhsoan.application.ucservice.followup;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.followup.FollowUpReminder;
import com.benhsoan.domain.followup.enums.ReminderType;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.followup.CreateFollowUpReminderCommand;
import com.benhsoan.port.dto.result.FollowUpReminderResult;
import com.benhsoan.port.inbound.followup.CreateFollowUpReminderUseCase;
import com.benhsoan.port.outbound.repository.followup.FollowUpReminderRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateFollowUpReminderService implements CreateFollowUpReminderUseCase {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final FollowUpReminderRepository followUpReminderRepository;
    private final VisitRepository visitRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final FollowUpReminderResultMapper resultMapper;
    private final FollowUpReminderAuthorizer authorizer;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;

    @Override
    public FollowUpReminderResult create(CreateFollowUpReminderCommand command) {
        authorizer.requireReceptionistOrAdmin();

        if (command == null) {
            throw new ValidationException("Command is required.");
        }
        if (command.visitId() == null) {
            throw new ValidationException("visitId is required.");
        }

        Visit visit = visitRepository.findById(command.visitId())
                .orElseThrow(() -> new VisitNotFoundException(command.visitId()));

        if (!visit.getPatientId().equals(command.patientId())) {
            throw new ValidationException("Visit does not belong to the specified patient.");
        }

        requireDoctorFollowUpIndication(command.visitId());

        if (command.followUpDate() == null) {
            throw new ValidationException("Follow-up date is required.");
        }
        if (command.followUpDate().isBefore(toVisitDate(visit))) {
            throw new ValidationException("Follow-up date must not be before the visit date.");
        }

        ReminderType type = command.reminderType() == null ? ReminderType.GENERAL : command.reminderType();

        FollowUpReminder reminder = FollowUpReminder.create(
                command.patientId(),
                command.visitId(),
                command.appointmentId(),
                command.followUpDate(),
                command.remindAt(),
                type,
                command.notes(),
                currentUserPort.getCurrentUserId(),
                clockPort.now()
        );

        return resultMapper.toResult(followUpReminderRepository.save(reminder));
    }

    private void requireDoctorFollowUpIndication(UUID visitId) {
        MedicalRecord medicalRecord = medicalRecordRepository.findByVisitId(visitId).orElse(null);
        if (medicalRecord == null
                || medicalRecord.getDoctorInstructions() == null
                || medicalRecord.getDoctorInstructions().isBlank()) {
            throw new ValidationException("Cannot create reminder: Visit has no follow-up indication from doctor.");
        }
    }

    private LocalDate toVisitDate(Visit visit) {
        Instant reference = visit.getCompletedAt() != null ? visit.getCompletedAt() : visit.getVisitAt();
        return reference.atZone(CLINIC_ZONE).toLocalDate();
    }
}

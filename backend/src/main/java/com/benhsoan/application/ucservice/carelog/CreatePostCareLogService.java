package com.benhsoan.application.ucservice.carelog;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.carelog.PostCareLog;
import com.benhsoan.domain.carelog.enums.ContactOutcome;
import com.benhsoan.domain.followup.FollowUpReminder;
import com.benhsoan.domain.followup.enums.ReminderStatus;
import com.benhsoan.domain.followup.exception.FollowUpReminderNotFoundException;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.carelog.CreatePostCareLogCommand;
import com.benhsoan.port.dto.result.PostCareLogResult;
import com.benhsoan.port.inbound.carelog.CreatePostCareLogUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.carelog.PostCareLogRepository;
import com.benhsoan.port.outbound.repository.followup.FollowUpReminderRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CreatePostCareLogService implements CreatePostCareLogUseCase {

    private final PostCareLogRepository postCareLogRepository;
    private final FollowUpReminderRepository followUpReminderRepository;
    private final PatientRepository patientRepository;
    private final AuditLogRepository auditLogRepository;
    private final PostCareLogResultMapper resultMapper;
    private final PostCareLogAuthorizer authorizer;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;

    @Override
    public PostCareLogResult create(CreatePostCareLogCommand command) {
        authorizer.requireStaffOrAdmin();

        if (command == null) {
            throw new ValidationException("Command is required.");
        }
        if (command.patientId() == null) {
            throw new ValidationException("patientId is required.");
        }

        patientRepository.findById(command.patientId())
                .orElseThrow(() -> new PatientNotFoundException(command.patientId()));

        FollowUpReminder reminder = resolveReminder(command);

        UUID actorId = currentUserPort.getCurrentUserId();

        PostCareLog careLog = PostCareLog.create(
                command.patientId(),
                command.reminderId(),
                command.visitId(),
                command.contactChannel(),
                command.contactedAt(),
                command.patientCondition(),
                command.careNotes(),
                command.contactOutcome(),
                actorId,
                clockPort.now()
        );

        PostCareLog saved = postCareLogRepository.save(careLog);

        if (reminder != null) {
            transitionReminder(reminder, command.contactOutcome());
        }

        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.CREATE,
                ResourceType.CARE_LOG,
                saved.getId(),
                detail(saved),
                null
        ));

        return resultMapper.toResult(saved);
    }

    private FollowUpReminder resolveReminder(CreatePostCareLogCommand command) {
        if (command.reminderId() == null) {
            return null;
        }
        FollowUpReminder reminder = followUpReminderRepository.findById(command.reminderId())
                .orElseThrow(() -> new FollowUpReminderNotFoundException(command.reminderId()));

        if (!reminder.getPatientId().equals(command.patientId())) {
            throw new ValidationException("Follow-up reminder does not belong to the specified patient.");
        }
        return reminder;
    }

    private void transitionReminder(FollowUpReminder reminder, ContactOutcome outcome) {
        ReminderStatus current = reminder.getStatus();
        if (current != ReminderStatus.PENDING && current != ReminderStatus.SENT) {
            return;
        }

        ReminderStatus target = outcome == ContactOutcome.REACHED
                ? ReminderStatus.COMPLETED
                : ReminderStatus.SENT;

        reminder.updateStatus(target);
        followUpReminderRepository.save(reminder);
    }

    private String detail(PostCareLog saved) {
        return "Post-care log recorded for patient "
                + saved.getPatientId()
                + " via "
                + saved.getContactChannel()
                + " (outcome: "
                + saved.getContactOutcome()
                + ").";
    }
}

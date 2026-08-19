package com.benhsoan.application.ucservice.followup;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.followup.FollowUpReminder;
import com.benhsoan.domain.followup.enums.ReminderType;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.followup.CreateFollowUpReminderCommand;
import com.benhsoan.port.dto.result.FollowUpReminderResult;
import com.benhsoan.port.inbound.followup.CreateFollowUpReminderUseCase;
import com.benhsoan.port.outbound.repository.followup.FollowUpReminderRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateFollowUpReminderService implements CreateFollowUpReminderUseCase {

    private final FollowUpReminderRepository followUpReminderRepository;
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
}

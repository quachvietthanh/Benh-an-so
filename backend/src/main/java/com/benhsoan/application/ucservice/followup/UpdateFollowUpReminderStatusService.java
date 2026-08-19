package com.benhsoan.application.ucservice.followup;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.followup.FollowUpReminder;
import com.benhsoan.domain.followup.enums.ReminderStatus;
import com.benhsoan.domain.followup.exception.FollowUpReminderNotFoundException;
import com.benhsoan.port.dto.result.FollowUpReminderResult;
import com.benhsoan.port.inbound.followup.UpdateFollowUpReminderStatusUseCase;
import com.benhsoan.port.outbound.repository.followup.FollowUpReminderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateFollowUpReminderStatusService implements UpdateFollowUpReminderStatusUseCase {

    private final FollowUpReminderRepository followUpReminderRepository;
    private final FollowUpReminderResultMapper resultMapper;
    private final FollowUpReminderAuthorizer authorizer;

    @Override
    public FollowUpReminderResult updateStatus(UUID reminderId, ReminderStatus newStatus) {
        authorizer.requireReceptionistOrAdmin();

        FollowUpReminder reminder = followUpReminderRepository.findById(reminderId)
                .orElseThrow(() -> new FollowUpReminderNotFoundException(reminderId));

        reminder.updateStatus(newStatus);

        return resultMapper.toResult(followUpReminderRepository.save(reminder));
    }
}

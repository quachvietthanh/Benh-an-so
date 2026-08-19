package com.benhsoan.port.inbound.followup;

import java.util.UUID;

import com.benhsoan.domain.followup.enums.ReminderStatus;
import com.benhsoan.port.dto.result.FollowUpReminderResult;

public interface UpdateFollowUpReminderStatusUseCase {

    FollowUpReminderResult updateStatus(UUID reminderId, ReminderStatus newStatus);
}

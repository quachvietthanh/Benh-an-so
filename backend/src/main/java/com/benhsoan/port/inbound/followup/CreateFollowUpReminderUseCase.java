package com.benhsoan.port.inbound.followup;

import com.benhsoan.port.dto.command.followup.CreateFollowUpReminderCommand;
import com.benhsoan.port.dto.result.FollowUpReminderResult;

public interface CreateFollowUpReminderUseCase {

    FollowUpReminderResult create(CreateFollowUpReminderCommand command);
}

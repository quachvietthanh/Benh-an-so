package com.benhsoan.port.inbound.medicalrecord;

import com.benhsoan.port.dto.command.medicalrecord.SendSigningReminderCommand;
import com.benhsoan.port.dto.result.SigningReminderResult;

public interface SendSigningReminderUseCase {

    SigningReminderResult sendReminder(SendSigningReminderCommand command);
}

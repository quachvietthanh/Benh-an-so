package com.benhsoan.port.inbound.medicalrecord;

import java.util.List;
import java.util.UUID;

import com.benhsoan.port.dto.result.SigningReminderResult;

public interface GetSigningRemindersUseCase {

    List<SigningReminderResult> getReminders(UUID medicalRecordId);
}

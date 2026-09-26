package com.benhsoan.domain.followup.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import java.util.UUID;


public class FollowUpReminderNotFoundException extends FollowUpReminderException {

    public FollowUpReminderNotFoundException(UUID reminderId) {
        super(DomainErrorCode.FOLLOW_UP_REMINDER_NOT_FOUND, "Follow-up reminder not found: " + reminderId);
    }
}

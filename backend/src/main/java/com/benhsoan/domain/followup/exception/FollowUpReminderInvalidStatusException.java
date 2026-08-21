package com.benhsoan.domain.followup.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class FollowUpReminderInvalidStatusException extends FollowUpReminderException {

    public FollowUpReminderInvalidStatusException(String message) {
        super(DomainErrorCode.FOLLOW_UP_REMINDER_INVALID_STATUS, message);
    }
}

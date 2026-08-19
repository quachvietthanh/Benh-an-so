package com.benhsoan.domain.followup.exception;

import org.springframework.http.HttpStatus;

public class FollowUpReminderInvalidStatusException extends FollowUpReminderException {

    public FollowUpReminderInvalidStatusException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}

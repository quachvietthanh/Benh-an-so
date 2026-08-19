package com.benhsoan.domain.followup.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

public class FollowUpReminderNotFoundException extends FollowUpReminderException {

    public FollowUpReminderNotFoundException(UUID reminderId) {
        super(HttpStatus.NOT_FOUND, "Follow-up reminder not found: " + reminderId);
    }
}

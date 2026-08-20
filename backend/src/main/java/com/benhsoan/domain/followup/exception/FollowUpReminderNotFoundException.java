package com.benhsoan.domain.followup.exception;

import java.util.UUID;


public class FollowUpReminderNotFoundException extends FollowUpReminderException {

    public FollowUpReminderNotFoundException(UUID reminderId) {
        super("Follow-up reminder not found: " + reminderId);
    }
}

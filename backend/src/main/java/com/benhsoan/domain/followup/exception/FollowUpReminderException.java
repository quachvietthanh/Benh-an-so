package com.benhsoan.domain.followup.exception;


import com.benhsoan.domain.shared.exception.DomainException;

public abstract class FollowUpReminderException extends DomainException {

    protected FollowUpReminderException(String message) {
        super(message);
    }
}

package com.benhsoan.domain.followup.exception;

import org.springframework.http.HttpStatus;

import com.benhsoan.domain.shared.exception.DomainException;

public abstract class FollowUpReminderException extends DomainException {

    protected FollowUpReminderException(HttpStatus status, String message) {
        super(status, message);
    }
}

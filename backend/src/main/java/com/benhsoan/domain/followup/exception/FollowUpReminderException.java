package com.benhsoan.domain.followup.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


import com.benhsoan.domain.shared.exception.DomainException;

public abstract class FollowUpReminderException extends DomainException {

    protected FollowUpReminderException(DomainErrorCode code, String message) {
        super(code, message);
    }
}

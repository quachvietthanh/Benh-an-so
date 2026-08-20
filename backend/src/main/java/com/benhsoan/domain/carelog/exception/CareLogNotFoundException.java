package com.benhsoan.domain.carelog.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import java.util.UUID;


public class CareLogNotFoundException extends CareLogException {

    public CareLogNotFoundException(UUID careLogId) {
        super(DomainErrorCode.CARE_LOG_NOT_FOUND, "Post-care log not found: " + careLogId);
    }
}

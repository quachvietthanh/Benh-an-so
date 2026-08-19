package com.benhsoan.domain.carelog.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

public class CareLogNotFoundException extends CareLogException {

    public CareLogNotFoundException(UUID careLogId) {
        super(HttpStatus.NOT_FOUND, "Post-care log not found: " + careLogId);
    }
}

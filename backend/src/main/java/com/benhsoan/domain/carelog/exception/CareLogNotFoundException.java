package com.benhsoan.domain.carelog.exception;

import java.util.UUID;


public class CareLogNotFoundException extends CareLogException {

    public CareLogNotFoundException(UUID careLogId) {
        super("Post-care log not found: " + careLogId);
    }
}

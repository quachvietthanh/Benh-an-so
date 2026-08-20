package com.benhsoan.domain.visit.exception;

import java.util.UUID;


public class VisitNotFoundException extends VisitException {

    public VisitNotFoundException(UUID visitId) {
        super("Visit not found: " + visitId);
    }
}

package com.benhsoan.domain.visit.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class VisitEncounterAccessDeniedException extends VisitException {

    public VisitEncounterAccessDeniedException() {
        super(DomainErrorCode.VISIT_ENCOUNTER_ACCESS_DENIED, "You do not have permission to view this visit encounter.");
    }
}

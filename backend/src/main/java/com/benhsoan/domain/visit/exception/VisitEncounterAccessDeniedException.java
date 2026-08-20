package com.benhsoan.domain.visit.exception;


public class VisitEncounterAccessDeniedException extends VisitException {

    public VisitEncounterAccessDeniedException() {
        super("You do not have permission to view this visit encounter.");
    }
}

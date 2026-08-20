package com.benhsoan.domain.visit.exception;


public class VisitAlreadyCompletedException extends VisitException {

    public VisitAlreadyCompletedException() {
        super("Visit has already been completed.");
    }
}

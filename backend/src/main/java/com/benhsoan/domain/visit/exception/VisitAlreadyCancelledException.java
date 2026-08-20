package com.benhsoan.domain.visit.exception;


public class VisitAlreadyCancelledException extends VisitException {

    public VisitAlreadyCancelledException() {
        super("Visit has already been cancelled.");
    }
}

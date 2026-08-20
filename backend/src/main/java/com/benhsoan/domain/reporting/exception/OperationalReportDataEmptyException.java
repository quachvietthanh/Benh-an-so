package com.benhsoan.domain.reporting.exception;


import com.benhsoan.domain.shared.exception.DomainException;

public class OperationalReportDataEmptyException extends DomainException {

    public OperationalReportDataEmptyException() {
        super("No report data available for the selected period.");
    }
}

package com.benhsoan.domain.reporting.exception;

import org.springframework.http.HttpStatus;

import com.benhsoan.domain.shared.exception.DomainException;

public class OperationalReportDataEmptyException extends DomainException {

    public OperationalReportDataEmptyException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "No report data available for the selected period.");
    }
}

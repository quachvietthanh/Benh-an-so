package com.benhsoan.domain.reporting.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


import com.benhsoan.domain.shared.exception.DomainException;

public class OperationalReportDataEmptyException extends DomainException {

    public OperationalReportDataEmptyException() {
        super(DomainErrorCode.REPORT_DATA_EMPTY, "No report data available for the selected period.");
    }
}

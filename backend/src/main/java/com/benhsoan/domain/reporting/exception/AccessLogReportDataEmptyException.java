package com.benhsoan.domain.reporting.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;
import com.benhsoan.domain.shared.exception.DomainException;

public class AccessLogReportDataEmptyException extends DomainException {

    public AccessLogReportDataEmptyException() {
        super(DomainErrorCode.REPORT_DATA_EMPTY, "No medical record access logs available for the selected period.");
    }
}

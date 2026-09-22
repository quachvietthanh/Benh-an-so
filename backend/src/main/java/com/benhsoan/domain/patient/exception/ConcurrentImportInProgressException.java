package com.benhsoan.domain.patient.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class ConcurrentImportInProgressException extends PatientException {

    public ConcurrentImportInProgressException() {
        super(DomainErrorCode.CONCURRENT_IMPORT_IN_PROGRESS,
                "Hệ thống đang thực hiện một tiến trình nhập hồ sơ khác, vui lòng thử lại sau.");
    }

    public ConcurrentImportInProgressException(String message) {
        super(DomainErrorCode.CONCURRENT_IMPORT_IN_PROGRESS, message);
    }
}

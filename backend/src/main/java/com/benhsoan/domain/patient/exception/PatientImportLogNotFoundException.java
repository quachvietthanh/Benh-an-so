package com.benhsoan.domain.patient.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class PatientImportLogNotFoundException extends PatientException {

    public PatientImportLogNotFoundException(UUID id) {
        super(DomainErrorCode.PATIENT_IMPORT_LOG_NOT_FOUND, "Không tìm thấy nhật ký nhập liệu với mã: " + id);
    }
}

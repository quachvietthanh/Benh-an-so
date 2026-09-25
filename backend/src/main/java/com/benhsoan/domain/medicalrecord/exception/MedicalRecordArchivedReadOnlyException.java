package com.benhsoan.domain.medicalrecord.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

/**
 * Thrown when an operation attempts to modify or delete an archived medical record
 * (QTN-19: Hồ sơ ở kho lưu trữ chỉ đọc, không sửa và không xóa).
 */
public class MedicalRecordArchivedReadOnlyException extends MedicalRecordException {

    public MedicalRecordArchivedReadOnlyException() {
        super(
                DomainErrorCode.MEDICAL_RECORD_ARCHIVED_READ_ONLY,
                "Hồ sơ bệnh án đã ở kho lưu trữ, chỉ cho xem, không được phép sửa đổi hoặc xóa theo QTN-19."
        );
    }

    public MedicalRecordArchivedReadOnlyException(String message) {
        super(
                DomainErrorCode.MEDICAL_RECORD_ARCHIVED_READ_ONLY,
                message != null ? message : "Hồ sơ bệnh án đã ở kho lưu trữ, chỉ cho xem, không được phép sửa đổi hoặc xóa theo QTN-19."
        );
    }
}

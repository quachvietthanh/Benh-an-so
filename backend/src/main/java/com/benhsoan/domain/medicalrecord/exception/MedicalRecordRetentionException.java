package com.benhsoan.domain.medicalrecord.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

/**
 * Thrown when a medical record is still inside the minimum retention period
 * (QTN-19) and therefore cannot be deleted yet.
 */
public class MedicalRecordRetentionException extends MedicalRecordException {

    public MedicalRecordRetentionException() {
        super(
                DomainErrorCode.MEDICAL_RECORD_IN_RETENTION_PERIOD,
                "Hồ sơ bệnh án còn trong thời hạn lưu trữ tối thiểu và không được phép xóa."
        );
    }
}

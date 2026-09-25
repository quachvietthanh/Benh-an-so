package com.benhsoan.domain.medicalrecord.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

/**
 * Thrown when a medical record is not yet eligible to be archived
 * (e.g. visit is not completed or has not passed the active record duration).
 */
public class MedicalRecordNotEligibleForArchiveException extends MedicalRecordException {

    public MedicalRecordNotEligibleForArchiveException(UUID recordId, String message) {
        super(
                DomainErrorCode.MEDICAL_RECORD_NOT_ELIGIBLE_FOR_ARCHIVE,
                message != null ? message : "Hồ sơ bệnh án chưa đủ điều kiện chuyển vào kho lưu trữ (chưa hết thời hạn hoạt động)."
        );
    }

    public MedicalRecordNotEligibleForArchiveException(String message) {
        super(
                DomainErrorCode.MEDICAL_RECORD_NOT_ELIGIBLE_FOR_ARCHIVE,
                message != null ? message : "Hồ sơ bệnh án chưa đủ điều kiện chuyển vào kho lưu trữ."
        );
    }
}

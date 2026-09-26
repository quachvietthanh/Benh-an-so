package com.benhsoan.domain.patient.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class PatientConsentAccessDeniedException extends PatientException {

    public PatientConsentAccessDeniedException() {
        super(
                DomainErrorCode.PATIENT_CONSENT_ACCESS_DENIED,
                "Chỉ Lễ tân hoặc Quản trị viên mới có quyền cập nhật hoặc rút phiếu đồng ý xử lý dữ liệu (QTN-24)."
        );
    }
}

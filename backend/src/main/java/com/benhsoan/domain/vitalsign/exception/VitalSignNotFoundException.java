package com.benhsoan.domain.vitalsign.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;
import com.benhsoan.domain.shared.exception.DomainException;

public class VitalSignNotFoundException extends DomainException {

    public VitalSignNotFoundException(UUID id) {
        super(DomainErrorCode.VITAL_SIGN_NOT_FOUND, "Không tìm thấy chỉ số sinh tồn với mã: " + id);
    }

    public VitalSignNotFoundException(String message) {
        super(DomainErrorCode.VITAL_SIGN_NOT_FOUND, message);
    }
}

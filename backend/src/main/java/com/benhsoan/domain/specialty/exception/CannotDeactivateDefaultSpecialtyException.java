package com.benhsoan.domain.specialty.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;
import com.benhsoan.domain.shared.exception.DomainException;

public class CannotDeactivateDefaultSpecialtyException extends DomainException {

    public CannotDeactivateDefaultSpecialtyException() {
        super(DomainErrorCode.CANNOT_DEACTIVATE_DEFAULT_SPECIALTY, "Không thể ngừng dùng chuyên khoa mặc định hệ thống (GENERAL).");
    }
}

package com.benhsoan.domain.portal.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;
import com.benhsoan.domain.shared.exception.DomainException;

/**
 * Thrown when a patient-portal lookup cannot resolve a ready examination result.
 * The message is intentionally generic so callers cannot distinguish between an
 * unknown appointment code and a visit that is not yet completed (QTN-15).
 */
public class PortalLookupNotFoundException extends DomainException {

    public PortalLookupNotFoundException() {
        super(DomainErrorCode.PORTAL_LOOKUP_NOT_FOUND, "Kết quả khám chưa sẵn sàng hoặc mã hẹn không hợp lệ");
    }
}

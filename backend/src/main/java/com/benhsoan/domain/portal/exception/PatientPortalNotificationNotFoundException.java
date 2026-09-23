package com.benhsoan.domain.portal.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;
import com.benhsoan.domain.shared.exception.DomainException;

/**
 * Thrown when a patient-portal notification does not exist for the requested id
 * (mapped to HTTP 404). Cross-patient access to an existing notification is
 * rejected separately by {@code PatientAccessGuard} as an explicit IDOR rejection
 * (HTTP 403 + ACCESS_DENIED audit), matching NCL-14-CN-008 TC-03 / QTN-23.
 */
public class PatientPortalNotificationNotFoundException extends DomainException {

    public PatientPortalNotificationNotFoundException() {
        super(DomainErrorCode.PATIENT_PORTAL_NOTIFICATION_NOT_FOUND, "Không tìm thấy thông báo");
    }
}

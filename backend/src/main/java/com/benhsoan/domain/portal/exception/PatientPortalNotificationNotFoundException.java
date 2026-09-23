package com.benhsoan.domain.portal.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;
import com.benhsoan.domain.shared.exception.DomainException;

/**
 * Thrown when a patient-portal notification cannot be resolved for the caller.
 * The message is intentionally generic (NCL-14-CN-008 TC-03 / QTN-23) so callers
 * cannot distinguish an unknown notification id from one that belongs to another
 * patient.
 */
public class PatientPortalNotificationNotFoundException extends DomainException {

    public PatientPortalNotificationNotFoundException() {
        super(DomainErrorCode.PATIENT_PORTAL_NOTIFICATION_NOT_FOUND, "Không tìm thấy thông báo");
    }
}

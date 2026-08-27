package com.benhsoan.domain.appointment.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

/**
 * The appointment start time has already passed, so the patient can no longer
 * cancel or reschedule it themselves (NCL-14-CN-004 TC-02).
 */
public class AppointmentPastCutoffException extends AppointmentException {

    public AppointmentPastCutoffException() {
        super(DomainErrorCode.APPOINTMENT_PAST_CUTOFF,
                "Đã quá giờ hẹn. Vui lòng liên hệ lễ tân để được hỗ trợ.");
    }
}

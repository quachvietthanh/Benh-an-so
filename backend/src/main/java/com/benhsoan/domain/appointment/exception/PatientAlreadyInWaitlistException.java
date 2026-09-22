package com.benhsoan.domain.appointment.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class PatientAlreadyInWaitlistException extends AppointmentException {

    public PatientAlreadyInWaitlistException() {
        super(DomainErrorCode.PATIENT_ALREADY_IN_WAITLIST,
                "Bệnh nhân đã có tên trong danh sách chờ của bác sĩ vào ngày này.");
    }

    public PatientAlreadyInWaitlistException(String message) {
        super(DomainErrorCode.PATIENT_ALREADY_IN_WAITLIST, message);
    }
}

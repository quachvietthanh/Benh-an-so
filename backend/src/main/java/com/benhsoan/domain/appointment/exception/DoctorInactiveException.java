package com.benhsoan.domain.appointment.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import java.util.UUID;


public class DoctorInactiveException extends AppointmentException {

    public DoctorInactiveException(UUID doctorId) {
        super(DomainErrorCode.DOCTOR_INACTIVE,
                "Doctor is inactive: " + doctorId
        );
    }

}

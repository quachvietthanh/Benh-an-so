package com.benhsoan.domain.appointment.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import java.util.UUID;


public class DoctorNotFoundException extends AppointmentException {

    public DoctorNotFoundException(UUID doctorId) {
        super(DomainErrorCode.DOCTOR_NOT_FOUND,
                "Doctor not found: " + doctorId
        );
    }

}

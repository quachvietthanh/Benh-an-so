package com.benhsoan.domain.appointment.exception;

import java.util.UUID;


public class DoctorNotFoundException extends AppointmentException {

    public DoctorNotFoundException(UUID doctorId) {
        super(
                "Doctor not found: " + doctorId
        );
    }

}

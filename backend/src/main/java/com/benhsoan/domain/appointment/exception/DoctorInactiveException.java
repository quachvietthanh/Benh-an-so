package com.benhsoan.domain.appointment.exception;

import java.util.UUID;


public class DoctorInactiveException extends AppointmentException {

    public DoctorInactiveException(UUID doctorId) {
        super(
                "Doctor is inactive: " + doctorId
        );
    }

}

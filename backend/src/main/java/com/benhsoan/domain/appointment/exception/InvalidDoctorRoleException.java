package com.benhsoan.domain.appointment.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

/**
 * The resolved user is not a doctor (NCL-14-CN-003 role validation).
 */
public class InvalidDoctorRoleException extends AppointmentException {

    public InvalidDoctorRoleException(UUID doctorId) {
        super(DomainErrorCode.INVALID_DOCTOR_ROLE,
                "Bác sĩ được chọn không hợp lệ: " + doctorId
        );
    }

}

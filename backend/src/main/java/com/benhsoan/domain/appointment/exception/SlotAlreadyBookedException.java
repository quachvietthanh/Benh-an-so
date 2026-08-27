package com.benhsoan.domain.appointment.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

/**
 * The selected slot was concurrently taken by another patient or by reception (QTN-04).
 */
public class SlotAlreadyBookedException extends AppointmentException {

    public SlotAlreadyBookedException() {
        super(DomainErrorCode.APPOINTMENT_TIME_CONFLICT,
                "Khung giờ này đã có người đặt. Vui lòng chọn khung giờ khác."
        );
    }

}

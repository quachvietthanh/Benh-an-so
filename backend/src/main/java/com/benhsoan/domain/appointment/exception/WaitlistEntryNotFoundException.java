package com.benhsoan.domain.appointment.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class WaitlistEntryNotFoundException extends AppointmentException {

    public WaitlistEntryNotFoundException() {
        super(DomainErrorCode.WAITLIST_ENTRY_NOT_FOUND,
                "Không tìm thấy thông tin danh sách chờ.");
    }

    public WaitlistEntryNotFoundException(String message) {
        super(DomainErrorCode.WAITLIST_ENTRY_NOT_FOUND, message);
    }
}

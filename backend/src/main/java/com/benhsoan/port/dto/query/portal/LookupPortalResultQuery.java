package com.benhsoan.port.dto.query.portal;

import com.benhsoan.domain.shared.exception.ValidationException;

public record LookupPortalResultQuery(String appointmentCode, String phoneNumber) {

    public LookupPortalResultQuery {
        if (appointmentCode == null || appointmentCode.isBlank()) {
            throw new ValidationException("appointmentCode is required.");
        }
        appointmentCode = appointmentCode.trim();
        phoneNumber = phoneNumber == null || phoneNumber.isBlank() ? null : phoneNumber.trim();
    }
}

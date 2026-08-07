package com.benhsoan.application.ucservice.medicine;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class MedicineManagementAuthorizer {

    private static final String PHARMACIST_ROLE = "PHARMACIST";

    private final CurrentUserPort currentUserPort;

    void requirePharmacist() {
        if (!currentUserPort.hasRole(PHARMACIST_ROLE)) {
            throw new AccessDeniedException(
                    "Only pharmacists are allowed to manage medicines."
            );
        }
    }
}

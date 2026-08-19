package com.benhsoan.application.ucservice.carelog;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class PostCareLogAuthorizer {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String RECEPTIONIST_ROLE = "RECEPTIONIST";
    private static final String DOCTOR_ROLE = "DOCTOR";

    private final CurrentUserPort currentUserPort;

    void requireStaffOrAdmin() {
        if (!currentUserPort.hasRole(ADMIN_ROLE)
                && !currentUserPort.hasRole(RECEPTIONIST_ROLE)
                && !currentUserPort.hasRole(DOCTOR_ROLE)) {
            throw new AccessDeniedException("Only clinic staff can manage post-care logs.");
        }
    }
}

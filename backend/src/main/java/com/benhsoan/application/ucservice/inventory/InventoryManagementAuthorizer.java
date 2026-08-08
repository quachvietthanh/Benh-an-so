package com.benhsoan.application.ucservice.inventory;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class InventoryManagementAuthorizer {

    private static final String PHARMACIST_ROLE = "PHARMACIST";
    private static final String ADMIN_ROLE = "ADMIN";

    private final CurrentUserPort currentUserPort;

    void requirePharmacistOrAdmin() {
        if (!currentUserPort.hasRole(PHARMACIST_ROLE)
                && !currentUserPort.hasRole(ADMIN_ROLE)) {
            throw new AccessDeniedException(
                    "Only pharmacists or admins are allowed to receive stock."
            );
        }
    }
}

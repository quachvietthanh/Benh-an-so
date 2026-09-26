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
    private static final String PHARMACY_UPDATE_PERMISSION = "PHARMACY_UPDATE";
    private static final String PHARMACY_READ_PERMISSION = "PHARMACY_READ";

    private final CurrentUserPort currentUserPort;

    void requireInventoryUpdate() {
        if (!currentUserPort.hasPermission(PHARMACY_UPDATE_PERMISSION)
                && !currentUserPort.hasRole(PHARMACIST_ROLE)
                && !currentUserPort.hasRole(ADMIN_ROLE)) {
            throw new AccessDeniedException(
                    "Only pharmacists or administrators with inventory update permissions are allowed to modify inventory stock.");
        }
    }

    void requireInventoryRead() {
        if (!currentUserPort.hasPermission(PHARMACY_UPDATE_PERMISSION)
                && !currentUserPort.hasPermission(PHARMACY_READ_PERMISSION)
                && !currentUserPort.hasRole(PHARMACIST_ROLE)
                && !currentUserPort.hasRole(ADMIN_ROLE)) {
            throw new AccessDeniedException(
                    "Only pharmacists or administrators with inventory permissions are allowed to view inventory stock.");
        }
    }

    void requirePharmacistOrAdmin() {
        requireInventoryRead();
    }
}

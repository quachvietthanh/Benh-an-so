package com.benhsoan.application.ucservice.backup;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class BackupAuthorizer {

    private static final String ADMIN_ROLE = "ADMIN";

    private final CurrentUserPort currentUserPort;

    void requireAdmin() {
        if (!currentUserPort.hasRole(ADMIN_ROLE)) {
            throw new AccessDeniedException("Only admins can perform this operation.");
        }
    }
}

package com.benhsoan.application.ucservice.followup;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class FollowUpReminderAuthorizer {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String RECEPTIONIST_ROLE = "RECEPTIONIST";

    private final CurrentUserPort currentUserPort;

    void requireReceptionistOrAdmin() {
        if (!currentUserPort.hasRole(ADMIN_ROLE) && !currentUserPort.hasRole(RECEPTIONIST_ROLE)) {
            throw new AccessDeniedException("Only receptionists or admins can manage follow-up reminders.");
        }
    }
}

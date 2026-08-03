package com.benhsoan.application.ucservice.visit;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.visit.exception.VisitEncounterAccessDeniedException;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class VisitEncounterAuthorization {

    private final CurrentUserPort currentUserPort;

    void requireReadAccess(UUID visitDoctorId) {
        if (currentUserPort.hasRole("ADMIN") || currentUserPort.hasRole("NURSE")) {
            return;
        }
        if (currentUserPort.hasRole("DOCTOR") && visitDoctorId.equals(currentUserPort.getCurrentUserId())) {
            return;
        }
        throw new VisitEncounterAccessDeniedException();
    }
}

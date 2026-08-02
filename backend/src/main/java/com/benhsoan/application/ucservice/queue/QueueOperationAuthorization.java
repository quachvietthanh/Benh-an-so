package com.benhsoan.application.ucservice.queue;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.queue.MedicalQueue;
import com.benhsoan.domain.queue.exception.UnauthorizedQueueOperationException;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class QueueOperationAuthorization {

    private final CurrentUserPort currentUserPort;

    void requireCallPermission(MedicalQueue queue) {
        if (currentUserPort.hasRole("ADMIN") || currentUserPort.hasRole("RECEPTIONIST")) {
            return;
        }
        requireOwningDoctor(queue);
    }

    void requireClinicalUpdatePermission(MedicalQueue queue) {
        if (currentUserPort.hasRole("ADMIN") || currentUserPort.hasRole("NURSE")) {
            return;
        }
        requireOwningDoctor(queue);
    }

    void requireCompletePermission(MedicalQueue queue) {
        if (currentUserPort.hasRole("ADMIN")) {
            return;
        }
        requireOwningDoctor(queue);
    }

    void requireSkipPermission(MedicalQueue queue) {
        if (currentUserPort.hasRole("ADMIN") || currentUserPort.hasRole("RECEPTIONIST")) {
            return;
        }
        requireOwningDoctor(queue);
    }

    void requireReadPermission(MedicalQueue queue) {
        requireReadPermission(queue.getDoctorId());
    }

    void requireReadPermission(UUID doctorId) {
        if (currentUserPort.hasRole("ADMIN") || currentUserPort.hasRole("NURSE")
                || currentUserPort.hasRole("RECEPTIONIST")) {
            return;
        }
        if (!currentUserPort.hasRole("DOCTOR") || !doctorId.equals(currentUserPort.getCurrentUserId())) {
            throw new UnauthorizedQueueOperationException();
        }
    }

    private void requireOwningDoctor(MedicalQueue queue) {
        if (!currentUserPort.hasRole("DOCTOR") || !queue.getDoctorId().equals(currentUserPort.getCurrentUserId())) {
            throw new UnauthorizedQueueOperationException();
        }
    }
}

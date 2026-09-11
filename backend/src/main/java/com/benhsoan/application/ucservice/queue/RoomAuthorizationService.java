package com.benhsoan.application.ucservice.queue;

import org.springframework.stereotype.Service;

import com.benhsoan.domain.queue.exception.UnauthorizedQueueOperationException;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class RoomAuthorizationService {

    private final CurrentUserPort currentUserPort;

    void requireReadAccess() {
        if (!currentUserPort.hasRole("ADMIN")
                && !currentUserPort.hasRole("DOCTOR")
                && !currentUserPort.hasRole("RECEPTIONIST")
                && !currentUserPort.hasRole("MANAGER")) {
            throw new UnauthorizedQueueOperationException();
        }
    }

    void requireManageAccess() {
        if (!currentUserPort.hasRole("ADMIN")) {
            throw new UnauthorizedQueueOperationException();
        }
    }
}

package com.benhsoan.application.ucservice.queue;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.queue.enums.QueueItemSourceType;
import com.benhsoan.domain.queue.exception.UnauthorizedQueueOperationException;
import com.benhsoan.port.dto.command.queue.CheckInWalkInCommand;
import com.benhsoan.port.dto.result.QueueCheckInResult;
import com.benhsoan.port.inbound.queue.CheckInWalkInUseCase;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CheckInWalkInService implements CheckInWalkInUseCase {

    private final QueueCheckInCoordinator queueCheckInCoordinator;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;

    @Override
    public QueueCheckInResult checkIn(CheckInWalkInCommand command) {
        requireReceptionPermission();
        return queueCheckInCoordinator.checkIn(command.patientId(), command.doctorId(), null,
                QueueItemSourceType.WALK_IN, command.reason(), command.note(), command.specialtyId(), currentUserPort.getCurrentUserId(),
                clockPort.now());
    }

    private void requireReceptionPermission() {
        if (!currentUserPort.hasRole("ADMIN") && !currentUserPort.hasRole("RECEPTIONIST")) {
            throw new UnauthorizedQueueOperationException();
        }
    }
}

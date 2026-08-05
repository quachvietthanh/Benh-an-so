package com.benhsoan.application.ucservice.queue;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.queue.Room;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.queue.exception.RoomCodeAlreadyExistsException;
import com.benhsoan.port.dto.command.queue.CreateRoomCommand;
import com.benhsoan.port.dto.result.RoomResult;
import com.benhsoan.port.inbound.queue.CreateRoomUseCase;
import com.benhsoan.port.outbound.repository.queue.RoomRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateRoomService implements CreateRoomUseCase {

    private final RoomRepository roomRepository;
    private final RoomAuthorizationService authorizationService;
    private final RoomResultMapper resultMapper;
    private final ClockPort clockPort;
    private final RoomAuditService auditService;

    @Override
    public RoomResult create(CreateRoomCommand command) {
        authorizationService.requireManageAccess();
        Room room = Room.create(command.code(), command.name(), clockPort.now());
        if (roomRepository.existsByCode(room.getCode())) {
            throw new RoomCodeAlreadyExistsException(room.getCode());
        }
        Room savedRoom = roomRepository.save(room);
        auditService.record(ActionType.CREATE, savedRoom);
        return resultMapper.toResult(savedRoom);
    }
}

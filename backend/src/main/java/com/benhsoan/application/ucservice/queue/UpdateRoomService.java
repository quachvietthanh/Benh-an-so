package com.benhsoan.application.ucservice.queue;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.queue.Room;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.queue.exception.RoomNotFoundException;
import com.benhsoan.port.dto.command.queue.UpdateRoomCommand;
import com.benhsoan.port.dto.result.RoomResult;
import com.benhsoan.port.inbound.queue.UpdateRoomUseCase;
import com.benhsoan.port.outbound.repository.queue.RoomRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateRoomService implements UpdateRoomUseCase {

    private final RoomRepository roomRepository;
    private final RoomAuthorizationService authorizationService;
    private final RoomResultMapper resultMapper;
    private final ClockPort clockPort;
    private final RoomAuditService auditService;

    @Override
    public RoomResult update(UpdateRoomCommand command) {
        authorizationService.requireManageAccess();
        Room room = roomRepository.findById(command.roomId())
                .orElseThrow(() -> new RoomNotFoundException(command.roomId()));
        room.updateName(command.name(), clockPort.now());
        Room savedRoom = roomRepository.save(room);
        auditService.record(ActionType.UPDATE, savedRoom);
        return resultMapper.toResult(savedRoom);
    }
}

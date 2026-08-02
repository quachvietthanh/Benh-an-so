package com.benhsoan.application.ucservice.queue;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.queue.Room;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.queue.exception.RoomNotFoundException;
import com.benhsoan.port.dto.result.RoomResult;
import com.benhsoan.port.inbound.queue.DeactivateRoomUseCase;
import com.benhsoan.port.outbound.repository.crudRepository.queue.RoomRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DeactivateRoomService implements DeactivateRoomUseCase {

    private final RoomRepository roomRepository;
    private final RoomAuthorizationService authorizationService;
    private final RoomResultMapper resultMapper;
    private final ClockPort clockPort;
    private final RoomAuditService auditService;

    @Override
    public RoomResult deactivate(UUID roomId) {
        authorizationService.requireManageAccess();
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new RoomNotFoundException(roomId));
        room.deactivate(clockPort.now());
        Room savedRoom = roomRepository.save(room);
        auditService.record(ActionType.DEACTIVATE, savedRoom);
        return resultMapper.toResult(savedRoom);
    }
}

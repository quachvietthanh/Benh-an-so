package com.benhsoan.application.ucservice.queue;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.queue.exception.RoomNotFoundException;
import com.benhsoan.port.dto.result.RoomResult;
import com.benhsoan.port.inbound.queue.GetRoomUseCase;
import com.benhsoan.port.outbound.repository.queue.RoomRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetRoomService implements GetRoomUseCase {

    private final RoomRepository roomRepository;
    private final RoomAuthorizationService authorizationService;
    private final RoomResultMapper resultMapper;

    @Override
    public RoomResult getById(UUID roomId) {
        authorizationService.requireReadAccess();
        return resultMapper.toResult(roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId)));
    }
}

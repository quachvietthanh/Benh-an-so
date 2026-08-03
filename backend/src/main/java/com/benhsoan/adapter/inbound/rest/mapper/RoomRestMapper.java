package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.queue.CreateRoomRequest;
import com.benhsoan.adapter.inbound.rest.request.queue.UpdateRoomRequest;
import com.benhsoan.adapter.inbound.rest.response.queue.RoomResponse;
import com.benhsoan.port.dto.command.queue.CreateRoomCommand;
import com.benhsoan.port.dto.command.queue.UpdateRoomCommand;
import com.benhsoan.port.dto.result.RoomResult;

@Component
public class RoomRestMapper {

    public CreateRoomCommand toCommand(CreateRoomRequest request) {
        return new CreateRoomCommand(request.code(), request.name());
    }

    public UpdateRoomCommand toCommand(UUID roomId, UpdateRoomRequest request) {
        return new UpdateRoomCommand(roomId, request.name());
    }

    public RoomResponse toResponse(RoomResult result) {
        return new RoomResponse(result.id(), result.code(), result.name(), result.active(), result.createdAt(),
                result.updatedAt());
    }
}

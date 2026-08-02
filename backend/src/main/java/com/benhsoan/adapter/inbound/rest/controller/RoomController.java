package com.benhsoan.adapter.inbound.rest.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.RoomRestMapper;
import com.benhsoan.adapter.inbound.rest.request.queue.CreateRoomRequest;
import com.benhsoan.adapter.inbound.rest.request.queue.UpdateRoomRequest;
import com.benhsoan.adapter.inbound.rest.response.queue.RoomResponse;
import com.benhsoan.port.dto.command.queue.SearchRoomsQuery;
import com.benhsoan.port.inbound.queue.ActivateRoomUseCase;
import com.benhsoan.port.inbound.queue.CreateRoomUseCase;
import com.benhsoan.port.inbound.queue.DeactivateRoomUseCase;
import com.benhsoan.port.inbound.queue.GetRoomUseCase;
import com.benhsoan.port.inbound.queue.SearchRoomsUseCase;
import com.benhsoan.port.inbound.queue.UpdateRoomUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
@Validated
public class RoomController {

    private final SearchRoomsUseCase searchRoomsUseCase;
    private final GetRoomUseCase getRoomUseCase;
    private final CreateRoomUseCase createRoomUseCase;
    private final UpdateRoomUseCase updateRoomUseCase;
    private final ActivateRoomUseCase activateRoomUseCase;
    private final DeactivateRoomUseCase deactivateRoomUseCase;
    private final RoomRestMapper mapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE', 'RECEPTIONIST')")
    public Page<RoomResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "true") Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return searchRoomsUseCase.search(new SearchRoomsQuery(keyword, active, page, size)).map(mapper::toResponse);
    }

    @GetMapping("/{roomId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE', 'RECEPTIONIST')")
    public RoomResponse getById(@PathVariable UUID roomId) {
        return mapper.toResponse(getRoomUseCase.getById(roomId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public RoomResponse create(@Valid @RequestBody CreateRoomRequest request) {
        return mapper.toResponse(createRoomUseCase.create(mapper.toCommand(request)));
    }

    @PutMapping("/{roomId}")
    @PreAuthorize("hasRole('ADMIN')")
    public RoomResponse update(@PathVariable UUID roomId, @Valid @RequestBody UpdateRoomRequest request) {
        return mapper.toResponse(updateRoomUseCase.update(mapper.toCommand(roomId, request)));
    }

    @PatchMapping("/{roomId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public RoomResponse activate(@PathVariable UUID roomId) {
        return mapper.toResponse(activateRoomUseCase.activate(roomId));
    }

    @PatchMapping("/{roomId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public RoomResponse deactivate(@PathVariable UUID roomId) {
        return mapper.toResponse(deactivateRoomUseCase.deactivate(roomId));
    }
}

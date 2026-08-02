package com.benhsoan.adapter.inbound.rest.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.QueueRestMapper;
import com.benhsoan.adapter.inbound.rest.request.queue.CheckInWalkInRequest;
import com.benhsoan.adapter.inbound.rest.request.queue.SkipQueueItemRequest;
import com.benhsoan.adapter.inbound.rest.request.queue.UpdateQueueItemStatusRequest;
import com.benhsoan.adapter.inbound.rest.response.queue.QueueCheckInResponse;
import com.benhsoan.adapter.inbound.rest.response.queue.QueueItemResponse;
import com.benhsoan.port.dto.command.queue.CallNextQueueItemCommand;
import com.benhsoan.port.dto.command.queue.CheckInAppointmentCommand;
import com.benhsoan.port.dto.command.queue.CompleteQueueItemCommand;
import com.benhsoan.port.dto.command.queue.GetMyQueueQuery;
import com.benhsoan.port.dto.command.queue.GetQueuesQuery;
import com.benhsoan.port.inbound.queue.CallNextQueueItemUseCase;
import com.benhsoan.port.inbound.queue.CheckInAppointmentUseCase;
import com.benhsoan.port.inbound.queue.CheckInWalkInUseCase;
import com.benhsoan.port.inbound.queue.CompleteQueueItemUseCase;
import com.benhsoan.port.inbound.queue.GetMyQueueUseCase;
import com.benhsoan.port.inbound.queue.GetQueueItemUseCase;
import com.benhsoan.port.inbound.queue.GetQueuesUseCase;
import com.benhsoan.port.inbound.queue.SkipQueueItemUseCase;
import com.benhsoan.port.inbound.queue.UpdateQueueItemStatusUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class QueueController {

    private final GetQueuesUseCase getQueuesUseCase;
    private final GetMyQueueUseCase getMyQueueUseCase;
    private final CheckInAppointmentUseCase checkInAppointmentUseCase;
    private final CheckInWalkInUseCase checkInWalkInUseCase;
    private final CallNextQueueItemUseCase callNextQueueItemUseCase;
    private final UpdateQueueItemStatusUseCase updateQueueItemStatusUseCase;
    private final CompleteQueueItemUseCase completeQueueItemUseCase;
    private final GetQueueItemUseCase getQueueItemUseCase;
    private final SkipQueueItemUseCase skipQueueItemUseCase;
    private final QueueRestMapper mapper;

    @GetMapping("/queues")
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE', 'RECEPTIONIST')")
    public List<QueueItemResponse> getQueues(@RequestParam LocalDate date, @RequestParam(required = false) UUID doctorId,
            @RequestParam(required = false) UUID roomId) {
        return getQueuesUseCase.getQueues(new GetQueuesQuery(date, doctorId, roomId)).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/queues/me")
    @PreAuthorize("hasRole('DOCTOR')")
    public List<QueueItemResponse> getMyQueue(@RequestParam LocalDate date) {
        return getMyQueueUseCase.getMyQueue(new GetMyQueueQuery(date)).stream().map(mapper::toResponse).toList();
    }

    @PostMapping("/appointments/{appointmentId}/check-in")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public QueueCheckInResponse checkInAppointment(@PathVariable UUID appointmentId) {
        return mapper.toResponse(checkInAppointmentUseCase.checkIn(new CheckInAppointmentCommand(appointmentId)));
    }

    @PostMapping("/queue-items/walk-in")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public QueueCheckInResponse checkInWalkIn(@Valid @RequestBody CheckInWalkInRequest request) {
        return mapper.toResponse(checkInWalkInUseCase.checkIn(mapper.toCommand(request)));
    }

    @PostMapping("/queues/{queueId}/call-next")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public QueueItemResponse callNext(@PathVariable UUID queueId) {
        return mapper.toResponse(callNextQueueItemUseCase.callNext(new CallNextQueueItemCommand(queueId)));
    }

    @PatchMapping("/queue-items/{itemId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE')")
    public QueueItemResponse updateStatus(@PathVariable UUID itemId,
            @Valid @RequestBody UpdateQueueItemStatusRequest request) {
        return mapper.toResponse(updateQueueItemStatusUseCase.updateStatus(mapper.toCommand(itemId, request)));
    }

    @PostMapping("/queue-items/{itemId}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public QueueItemResponse complete(@PathVariable UUID itemId) {
        return mapper.toResponse(completeQueueItemUseCase.complete(new CompleteQueueItemCommand(itemId)));
    }

    @PostMapping("/queue-items/{itemId}/skip")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public QueueItemResponse skip(@PathVariable UUID itemId, @Valid @RequestBody SkipQueueItemRequest request) {
        return mapper.toResponse(skipQueueItemUseCase.skip(mapper.toCommand(itemId, request)));
    }

    @GetMapping("/queue-items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE', 'RECEPTIONIST')")
    public QueueItemResponse getById(@PathVariable UUID itemId) {
        return mapper.toResponse(getQueueItemUseCase.getById(itemId));
    }
}

package com.benhsoan.adapter.inbound.rest.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.MedicalQueueRestMapper;
import com.benhsoan.adapter.inbound.rest.response.queue.MedicalQueueResponse;
import com.benhsoan.port.dto.command.queue.CallNextCommand;
import com.benhsoan.port.dto.command.queue.GetQueueListQuery;
import com.benhsoan.port.dto.result.PageResponse;
import com.benhsoan.port.dto.result.QueueResult;
import com.benhsoan.port.inbound.queue.CallNextUseCase;
import com.benhsoan.port.inbound.queue.GetQueueListUseCase;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

/**
 * Legacy compatibility controller mapped to /api/v1/appointments/queue
 * for frontend backward compatibility.
 *
 * The new official Queue endpoints are at /api/v1/queue
 * (see {@link MedicalQueueController}).
 *
 * This controller delegates to the same use cases as the new controller,
 * ensuring data consistency regardless of which endpoint the caller uses.
 *
 * @see MedicalQueueController
 */
@RestController
@RequestMapping("/api/v1/appointments/queue")
@RequiredArgsConstructor
public class LegacyAppointmentQueueController {

    private final GetQueueListUseCase getQueueListUseCase;

    private final CallNextUseCase callNextUseCase;

    private final MedicalQueueRestMapper mapper;

    private final CurrentUserPort currentUserPort;

    /**
     * GET /api/v1/appointments/queue
     *
     * Legacy endpoint: returns all active queue items as a flat JSON array.
     * Frontend expects response.data to be an array of queue items
     * with fields: id, patientName, doctorName, status, checkedInAt, etc.
     *
     * Delegates to GetQueueListUseCase with default room to get all active queues.
     * Returns only WAITING and IN_PROGRESS items so the frontend sees
     * the current queue state.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MedicalQueueResponse>> getQueue() {
        // Fetch all active queue items (no room filter = system-wide)
        GetQueueListQuery query = new GetQueueListQuery(
                null,   // roomNumber - null = all rooms
                null,   // doctorId - null = all doctors
                null,   // status - null = all statuses
                0,      // page
                200     // size - large enough for a clinic day
        );

        PageResponse<QueueResult> resultPage = getQueueListUseCase.getQueueList(query);

        List<MedicalQueueResponse> responses = mapper.toResponse(
                resultPage.content()
        );

        return ResponseEntity.ok(responses);
    }

    /**
     * POST /api/v1/appointments/queue/call-next
     *
     * Legacy endpoint: calls the next WAITING patient.
     * The doctor ID is extracted from the currently authenticated user.
     * Uses "DEFAULT" as the room number since the legacy frontend
     * does not send a room.
     *
     * Delegates to CallNextUseCase.
     */
    @PostMapping("/call-next")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<MedicalQueueResponse> callNext() {
        UUID doctorId = currentUserPort.getCurrentUserId();

        CallNextCommand command = new CallNextCommand(
                doctorId,
                null  // roomNumber - null means system will pick first available
        );

        QueueResult result = callNextUseCase.callNext(command);

        return ResponseEntity.ok(mapper.toResponse(result));
    }
}

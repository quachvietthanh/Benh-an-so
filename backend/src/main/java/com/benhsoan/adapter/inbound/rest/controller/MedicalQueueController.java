package com.benhsoan.adapter.inbound.rest.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.MedicalQueueRestMapper;
import com.benhsoan.adapter.inbound.rest.request.queue.AddToQueueRequest;
import com.benhsoan.adapter.inbound.rest.request.queue.CallNextRequest;
import com.benhsoan.adapter.inbound.rest.request.queue.UpdateQueueStatusRequest;
import com.benhsoan.adapter.inbound.rest.response.queue.MedicalQueueResponse;
import com.benhsoan.domain.queue.enums.QueueStatus;
import com.benhsoan.port.dto.command.queue.GetQueueListQuery;
import com.benhsoan.port.dto.result.PageResponse;
import com.benhsoan.port.dto.result.QueueResult;
import com.benhsoan.port.inbound.queue.AddToQueueUseCase;
import com.benhsoan.port.inbound.queue.CallNextUseCase;
import com.benhsoan.port.inbound.queue.GetQueueListUseCase;
import com.benhsoan.port.inbound.queue.UpdateQueueStatusUseCase;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/queue")
@RequiredArgsConstructor
public class MedicalQueueController {

    private final AddToQueueUseCase addToQueueUseCase;

    private final CallNextUseCase callNextUseCase;

    private final UpdateQueueStatusUseCase updateQueueStatusUseCase;

    private final GetQueueListUseCase getQueueListUseCase;

    private final MedicalQueueRestMapper mapper;

    private final CurrentUserPort currentUserPort;

    /**
     * Thêm bệnh nhân vào hàng đợi, tự động cấp số thứ tự kế tiếp trong ngày.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<MedicalQueueResponse> addToQueue(
            @Valid @RequestBody AddToQueueRequest request
    ) {
        UUID createdBy = currentUserPort.getCurrentUserId();

        QueueResult result = addToQueueUseCase.addToQueue(
                mapper.toCommand(request, createdBy)
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toResponse(result));
    }

    /**
     * Gọi bệnh nhân WAITING kế tiếp trong phòng lên IN_PROGRESS.
     */
    @PostMapping("/call-next")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<MedicalQueueResponse> callNext(
            @Valid @RequestBody CallNextRequest request
    ) {
        QueueResult result = callNextUseCase.callNext(
                mapper.toCommand(request)
        );

        return ResponseEntity.ok(mapper.toResponse(result));
    }

    /**
     * Chuyển trạng thái queue item (IN_PROGRESS, WAITING_FOR_RESULT, COMPLETED, CANCELLED).
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE')")
    public ResponseEntity<MedicalQueueResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateQueueStatusRequest request
    ) {
        QueueResult result = updateQueueStatusUseCase.updateStatus(
                mapper.toCommand(id, request)
        );

        return ResponseEntity.ok(mapper.toResponse(result));
    }

    /**
     * Lấy danh sách hàng đợi theo phòng khám, có phân trang và lọc theo trạng thái.
     */
    @GetMapping("/room/{roomNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE', 'RECEPTIONIST', 'PHARMACIST')")
    public ResponseEntity<PageResponse<MedicalQueueResponse>> getQueueByRoom(
            @PathVariable String roomNumber,
            @RequestParam(required = false) QueueStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        GetQueueListQuery query = GetQueueListQuery.byRoom(
                roomNumber, status, page, size
        );

        PageResponse<QueueResult> resultPage = getQueueListUseCase.getQueueList(query);

        return ResponseEntity.ok(mapper.toPageResponse(resultPage));
    }

    /**
     * Lấy danh sách hàng đợi theo bác sĩ, có phân trang và lọc theo trạng thái.
     */
    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<PageResponse<MedicalQueueResponse>> getQueueByDoctor(
            @PathVariable UUID doctorId,
            @RequestParam(required = false) QueueStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        GetQueueListQuery query = GetQueueListQuery.byDoctor(
                doctorId, status, page, size
        );

        PageResponse<QueueResult> resultPage = getQueueListUseCase.getQueueList(query);

        return ResponseEntity.ok(mapper.toPageResponse(resultPage));
    }

    /**
     * Đếm số lượng queue items theo phòng, bác sĩ hoặc trạng thái.
     */
    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE', 'RECEPTIONIST', 'PHARMACIST')")
    public ResponseEntity<Long> count(
            @RequestParam(required = false) String roomNumber,
            @RequestParam(required = false) UUID doctorId,
            @RequestParam(required = false) QueueStatus status
    ) {
        GetQueueListQuery query = new GetQueueListQuery(
                roomNumber, doctorId, status, 0, 1
        );

        return ResponseEntity.ok(
                getQueueListUseCase.count(query)
        );
    }
}

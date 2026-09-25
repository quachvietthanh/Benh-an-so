package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.UUID;

import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.queue.CheckInWalkInRequest;
import com.benhsoan.adapter.inbound.rest.request.queue.CloseQueueItemRequest;
import com.benhsoan.adapter.inbound.rest.request.queue.PrioritizeQueueItemRequest;
import com.benhsoan.adapter.inbound.rest.request.queue.SkipQueueItemRequest;
import com.benhsoan.adapter.inbound.rest.request.queue.UpdateQueueItemStatusRequest;
import com.benhsoan.adapter.inbound.rest.response.queue.QueueCheckInResponse;
import com.benhsoan.adapter.inbound.rest.response.queue.QueueItemResponse;
import com.benhsoan.domain.patient.PatientAnonymizer;
import com.benhsoan.port.dto.command.queue.CheckInWalkInCommand;
import com.benhsoan.port.dto.command.queue.CloseVisitCommand;
import com.benhsoan.port.dto.command.queue.PrioritizeQueueItemCommand;
import com.benhsoan.port.dto.command.queue.SkipQueueItemCommand;
import com.benhsoan.port.dto.command.queue.UpdateQueueItemStatusCommand;
import com.benhsoan.port.dto.result.QueueCheckInResult;
import com.benhsoan.port.dto.result.QueueItemResult;

@Component
public class QueueRestMapper {

    private final AnonymizationModeState anonymizationModeState;

    public QueueRestMapper(
            AnonymizationModeState anonymizationModeState) {
        this.anonymizationModeState = anonymizationModeState;
    }

    public CheckInWalkInCommand toCommand(CheckInWalkInRequest request) {
        return new CheckInWalkInCommand(request.patientId(), request.doctorId(), request.reason(), request.note(),
                request.specialtyId());
    }

    public UpdateQueueItemStatusCommand toCommand(UUID queueItemId, UpdateQueueItemStatusRequest request) {
        return new UpdateQueueItemStatusCommand(queueItemId, request.targetStatus(), request.cancelReason());
    }

    public CloseVisitCommand toCommand(UUID queueItemId, CloseQueueItemRequest request) {
        return new CloseVisitCommand(queueItemId, request.outcome(), request.reason());
    }

    public static final String DEFAULT_SKIP_REASON = "Bệnh nhân vắng mặt khi gọi tên";

    public SkipQueueItemCommand toCommand(UUID queueItemId, SkipQueueItemRequest request) {
        String reason = (request != null && request.reason() != null && !request.reason().isBlank())
                ? request.reason().trim()
                : DEFAULT_SKIP_REASON;
        return new SkipQueueItemCommand(queueItemId, reason);
    }

    public PrioritizeQueueItemCommand toCommand(UUID queueItemId, PrioritizeQueueItemRequest request) {
        return new PrioritizeQueueItemCommand(queueItemId, request.priority(), request.reason());
    }

    public QueueItemResponse toResponse(QueueItemResult result) {
        return new QueueItemResponse(result.id(), result.medicalQueueId(), result.patientId(),
                anonymizationModeState.isEnabled() ? PatientAnonymizer.maskFullName(result.patientCode()) : result.patientName(),
                result.doctorId(), result.doctorName(), result.roomId(), result.roomNumber(), result.appointmentId(),
                result.visitId(), result.visitCode(), result.sourceType(), result.status(), result.queueNumber(), result.queueDate(),
                result.checkedInAt(), result.calledAt(), result.completedAt(), result.cancelledAt(), result.cancelReason(),
                result.skippedAt(), result.skipReason(), result.callCount(),
                result.priority(), result.priorityReason(), result.prioritizedAt(), result.prioritizedBy());
    }

    public QueueCheckInResponse toResponse(QueueCheckInResult result) {
        return new QueueCheckInResponse(result.queueItemId(), result.medicalQueueId(), result.visitId(),
                result.visitCode(), result.appointmentId(), result.patientId(), result.doctorId(), result.roomId(),
                result.queueNumber(), result.queueDate(), result.sourceType(), result.queueItemStatus(),
                result.visitStatus(), result.checkedInAt());
    }

    public com.benhsoan.adapter.inbound.rest.response.queue.QueueHistoryResponse toResponse(
            com.benhsoan.port.dto.result.QueueHistoryResult result) {
        return new com.benhsoan.adapter.inbound.rest.response.queue.QueueHistoryResponse(
                result.id(),
                result.queueItemId(),
                result.operatorId(),
                result.operatorName(),
                result.action(),
                result.status(),
                result.callCount(),
                result.reason(),
                result.timestamp());
    }

    public com.benhsoan.adapter.inbound.rest.response.queue.WaitingRoomBoardResponse toResponse(
            com.benhsoan.port.dto.result.WaitingRoomBoardResult result) {
        if (result == null) {
            return null;
        }
        return new com.benhsoan.adapter.inbound.rest.response.queue.WaitingRoomBoardResponse(
                result.date(),
                result.updatedAt(),
                result.rooms().stream().map(this::toResponse).toList()
        );
    }

    public com.benhsoan.adapter.inbound.rest.response.queue.RoomQueueDisplayResponse toResponse(
            com.benhsoan.port.dto.result.RoomQueueDisplayResult result) {
        if (result == null) {
            return null;
        }
        return new com.benhsoan.adapter.inbound.rest.response.queue.RoomQueueDisplayResponse(
                result.roomId(),
                result.roomNumber(),
                result.roomName(),
                result.doctorId(),
                result.doctorName(),
                toResponse(result.currentCalling()),
                result.waitingList().stream().map(this::toResponse).toList()
        );
    }

    public com.benhsoan.adapter.inbound.rest.response.queue.QueueDisplayItemResponse toResponse(
            com.benhsoan.port.dto.result.QueueDisplayItemResult result) {
        if (result == null) {
            return null;
        }
        return new com.benhsoan.adapter.inbound.rest.response.queue.QueueDisplayItemResponse(
                result.id(),
                result.queueNumber(),
                result.patientInitials(),
                result.status(),
                result.priority(),
                result.calledAt()
        );
    }
}

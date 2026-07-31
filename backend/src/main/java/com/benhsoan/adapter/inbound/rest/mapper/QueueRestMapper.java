package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.queue.CheckInWalkInRequest;
import com.benhsoan.adapter.inbound.rest.request.queue.UpdateQueueItemStatusRequest;
import com.benhsoan.adapter.inbound.rest.response.queue.QueueCheckInResponse;
import com.benhsoan.adapter.inbound.rest.response.queue.QueueItemResponse;
import com.benhsoan.port.dto.command.queue.CheckInWalkInCommand;
import com.benhsoan.port.dto.command.queue.UpdateQueueItemStatusCommand;
import com.benhsoan.port.dto.result.QueueCheckInResult;
import com.benhsoan.port.dto.result.QueueItemResult;

@Component
public class QueueRestMapper {

    public CheckInWalkInCommand toCommand(CheckInWalkInRequest request) {
        return new CheckInWalkInCommand(request.patientId(), request.doctorId(), request.reason(), request.note());
    }

    public UpdateQueueItemStatusCommand toCommand(UUID queueItemId, UpdateQueueItemStatusRequest request) {
        return new UpdateQueueItemStatusCommand(queueItemId, request.targetStatus(), request.cancelReason());
    }

    public QueueItemResponse toResponse(QueueItemResult result) {
        return new QueueItemResponse(result.id(), result.medicalQueueId(), result.patientId(), result.appointmentId(),
                result.visitId(), result.sourceType(), result.status(), result.queueNumber(), result.queueDate(),
                result.checkedInAt(), result.calledAt(), result.completedAt(), result.cancelledAt(), result.cancelReason());
    }

    public QueueCheckInResponse toResponse(QueueCheckInResult result) {
        return new QueueCheckInResponse(result.queueItemId(), result.medicalQueueId(), result.visitId(),
                result.visitCode(), result.appointmentId(), result.patientId(), result.doctorId(), result.roomId(),
                result.queueNumber(), result.queueDate(), result.sourceType(), result.queueItemStatus(),
                result.visitStatus(), result.checkedInAt());
    }
}

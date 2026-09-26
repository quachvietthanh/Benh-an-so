package com.benhsoan.adapter.inbound.rest.mapper;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.followup.CreateFollowUpReminderRequest;
import com.benhsoan.adapter.inbound.rest.response.followup.FollowUpReminderResponse;
import com.benhsoan.port.dto.command.followup.CreateFollowUpReminderCommand;
import com.benhsoan.port.dto.result.FollowUpReminderResult;

@Component
public class FollowUpReminderRestMapper {

    public FollowUpReminderResponse toResponse(FollowUpReminderResult result) {
        return new FollowUpReminderResponse(
                result.id(),
                result.patientId(),
                result.visitId(),
                result.appointmentId(),
                result.followUpDate(),
                result.remindAt(),
                result.reminderType(),
                result.status(),
                result.notes(),
                result.createdBy(),
                result.createdAt()
        );
    }

    public Page<FollowUpReminderResponse> toResponse(Page<FollowUpReminderResult> results) {
        return results.map(this::toResponse);
    }

    public CreateFollowUpReminderCommand toCommand(CreateFollowUpReminderRequest request) {
        return new CreateFollowUpReminderCommand(
                request.patientId(),
                request.visitId(),
                request.appointmentId(),
                request.followUpDate(),
                request.remindAt(),
                request.reminderType(),
                request.notes()
        );
    }
}

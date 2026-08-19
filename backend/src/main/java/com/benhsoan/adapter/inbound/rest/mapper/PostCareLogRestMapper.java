package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.carelog.CreatePostCareLogRequest;
import com.benhsoan.adapter.inbound.rest.response.carelog.PostCareLogResponse;
import com.benhsoan.port.dto.command.carelog.CreatePostCareLogCommand;
import com.benhsoan.port.dto.result.PostCareLogResult;

@Component
public class PostCareLogRestMapper {

    public PostCareLogResponse toResponse(PostCareLogResult result) {
        if (result == null) {
            return null;
        }
        return new PostCareLogResponse(
                result.id(),
                result.patientId(),
                result.reminderId(),
                result.visitId(),
                result.contactChannel(),
                result.contactedAt(),
                result.patientCondition(),
                result.careNotes(),
                result.contactOutcome(),
                result.performedBy(),
                result.createdAt()
        );
    }

    public List<PostCareLogResponse> toResponse(List<PostCareLogResult> results) {
        return results.stream().map(this::toResponse).toList();
    }

    public Page<PostCareLogResponse> toResponse(Page<PostCareLogResult> results) {
        return results.map(this::toResponse);
    }

    public CreatePostCareLogCommand toCommand(CreatePostCareLogRequest request) {
        return new CreatePostCareLogCommand(
                request.patientId(),
                request.reminderId(),
                request.visitId(),
                request.contactChannel(),
                request.contactedAt(),
                request.patientCondition(),
                request.careNotes(),
                request.contactOutcome()
        );
    }
}

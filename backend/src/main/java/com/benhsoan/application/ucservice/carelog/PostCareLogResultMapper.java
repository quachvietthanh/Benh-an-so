package com.benhsoan.application.ucservice.carelog;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.carelog.PostCareLog;
import com.benhsoan.port.dto.result.PostCareLogResult;

@Component
public class PostCareLogResultMapper {

    public PostCareLogResult toResult(PostCareLog careLog) {
        if (careLog == null) {
            return null;
        }
        return new PostCareLogResult(
                careLog.getId(),
                careLog.getPatientId(),
                careLog.getReminderId(),
                careLog.getVisitId(),
                careLog.getContactChannel(),
                careLog.getContactedAt(),
                careLog.getPatientCondition(),
                careLog.getCareNotes(),
                careLog.getContactOutcome(),
                careLog.getPerformedBy(),
                careLog.getCreatedAt()
        );
    }
}

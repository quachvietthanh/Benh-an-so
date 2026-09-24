package com.benhsoan.adapter.inbound.rest.mapper;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.response.session.SessionStatusResponse;
import com.benhsoan.adapter.inbound.rest.response.session.SessionSummaryResponse;
import com.benhsoan.port.dto.result.session.SessionStatusResult;
import com.benhsoan.port.dto.result.session.SessionSummaryResult;

@Component
public class SessionRestMapper {

    public SessionStatusResponse toStatusResponse(SessionStatusResult result) {
        return new SessionStatusResponse(
                result.sessionId(),
                result.userId(),
                result.username(),
                result.fullName(),
                result.role(),
                result.createdAt(),
                result.lastActivityAt(),
                result.expiresAt(),
                result.warningAt(),
                result.refreshExpiresAt(),
                result.inactivityTimeoutSeconds(),
                result.revoked()
        );
    }

    public Page<SessionSummaryResponse> toSummaryResponse(Page<SessionSummaryResult> results) {
        return results.map(this::toSummaryResponse);
    }

    public SessionSummaryResponse toSummaryResponse(SessionSummaryResult result) {
        return new SessionSummaryResponse(
                result.sessionId(),
                result.userId(),
                result.username(),
                result.fullName(),
                result.role(),
                result.createdAt(),
                result.lastActivityAt(),
                result.expiresAt(),
                result.refreshExpiresAt()
        );
    }
}
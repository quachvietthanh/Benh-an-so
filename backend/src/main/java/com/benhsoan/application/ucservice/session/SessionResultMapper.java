package com.benhsoan.application.ucservice.session;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.UserSession;
import com.benhsoan.domain.session.SessionSettings;
import com.benhsoan.port.dto.result.session.SessionStatusResult;
import com.benhsoan.port.dto.result.session.SessionSummaryResult;

@Component
public class SessionResultMapper {

    public SessionStatusResult toStatus(UserSession session, User user, Role role, SessionSettings settings) {
        Instant base = session.getLastUsedAt() != null ? session.getLastUsedAt() : session.getCreatedAt();
        Instant expiresAt = base.plus(settings.inactivityTimeout());
        Instant warningAt = expiresAt.minus(settings.warningThreshold());
        return new SessionStatusResult(
                session.getId(),
                session.getUserId(),
                user == null ? null : user.getUsername(),
                user == null ? null : user.getFullName(),
                role == null ? null : role.getName(),
                session.getCreatedAt(),
                session.getLastUsedAt(),
                expiresAt,
                warningAt,
                session.getRefreshExpiresAt(),
                settings.inactivityTimeout().toSeconds(),
                session.isRevoked()
        );
    }

    public SessionSummaryResult toSummary(UserSession session, User user, Role role, SessionSettings settings) {
        Instant base = session.getLastUsedAt() != null ? session.getLastUsedAt() : session.getCreatedAt();
        Instant expiresAt = base.plus(settings.inactivityTimeout());
        return new SessionSummaryResult(
                session.getId(),
                session.getUserId(),
                user == null ? null : user.getUsername(),
                user == null ? null : user.getFullName(),
                role == null ? null : role.getName(),
                session.getCreatedAt(),
                session.getLastUsedAt(),
                expiresAt,
                session.getRefreshExpiresAt()
        );
    }
}
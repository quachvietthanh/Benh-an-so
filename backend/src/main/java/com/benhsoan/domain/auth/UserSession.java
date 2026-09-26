package com.benhsoan.domain.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.shared.Guard.Guard;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString(exclude = {"refreshTokenHash", "previousRefreshTokenHash"})
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSession {

    private UUID id;

    private UUID userId;

    private String refreshTokenHash;

    private String previousRefreshTokenHash;

    private Instant refreshExpiresAt;

    private Instant createdAt;

    private Instant lastUsedAt;

    private Instant revokedAt;

    private String ipAddress;

    private String userAgent;

    private UserSession(
            UUID id,
            UUID userId,
            String refreshTokenHash,
            String previousRefreshTokenHash,
            Instant refreshExpiresAt,
            Instant createdAt,
            Instant lastUsedAt,
            Instant revokedAt,
            String ipAddress,
            String userAgent
    ) {
        this.id = Guard.require(id, "Session id");
        this.userId = Guard.require(userId, "User id");
        this.refreshTokenHash = Guard.require(refreshTokenHash, "Refresh token hash");
        this.previousRefreshTokenHash = previousRefreshTokenHash;
        this.refreshExpiresAt = Guard.require(refreshExpiresAt, "Refresh expires at");
        this.createdAt = Guard.require(createdAt, "Created at");

        this.lastUsedAt = lastUsedAt;
        this.revokedAt = revokedAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public static UserSession create(
            UUID userId,
            String refreshTokenHash,
            Instant refreshExpiresAt
    ) {
        return create(userId, refreshTokenHash, refreshExpiresAt, null, null);
    }

    public static UserSession create(
            UUID userId,
            String refreshTokenHash,
            Instant refreshExpiresAt,
            String ipAddress,
            String userAgent
    ) {
        Instant now = Instant.now();

        return new UserSession(
                UUID.randomUUID(),
                userId,
                refreshTokenHash,
                null,
                refreshExpiresAt,
                now,
                now,
                null,
                ipAddress,
                userAgent
        );
    }

    public void updateLastUsed(Instant now) {
        this.lastUsedAt = now;
    }

    public void revoke(Instant now) {
        this.revokedAt = now;
    }

    public boolean isRefreshExpired(Instant now) {
        return now.isAfter(refreshExpiresAt);
    }

    public boolean isIdleTimeout(
            Instant now,
            Duration timeout
    ) {
        if (timeout == null) {
            return false;
        }
        Instant referenceTime = lastUsedAt != null ? lastUsedAt : createdAt;
        return referenceTime != null && now.isAfter(referenceTime.plus(timeout));
    }

    public boolean isActive(
            Instant now,
            Duration timeout
    ) {
        return !isRefreshExpired(now)
                && !isRevoked()
                && !isIdleTimeout(now, timeout);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public void refresh(Duration timeout) {
        Instant now = Instant.now();
        this.lastUsedAt = now;
        this.refreshExpiresAt = now.plus(timeout);
    }

    public void extend(Instant now, Duration idleTimeout) {
        if (isRevoked()) {
            throw new com.benhsoan.domain.shared.exception.ValidationException("Cannot extend a revoked session.");
        }
        if (isRefreshExpired(now)) {
            throw new com.benhsoan.domain.shared.exception.ValidationException("Cannot extend an expired session.");
        }
        if (idleTimeout != null && isIdleTimeout(now, idleTimeout)) {
            throw new com.benhsoan.domain.shared.exception.ValidationException("Cannot extend an idle timed-out session.");
        }
        this.lastUsedAt = Guard.require(now, "Now");
    }

    public boolean matchesPreviousRefreshTokenHash(String refreshTokenHash) {
        return previousRefreshTokenHash != null
                && previousRefreshTokenHash.equals(refreshTokenHash);
    }

    public void rotateRefreshToken(
            String refreshTokenHash,
            Instant refreshExpiresAt,
            Instant lastUsedAt
    ) {
        this.previousRefreshTokenHash = this.refreshTokenHash;
        this.refreshTokenHash = Guard.require(refreshTokenHash, "Refresh token hash");
        this.refreshExpiresAt = Guard.require(refreshExpiresAt, "Refresh expiration");
        this.lastUsedAt = Guard.require(lastUsedAt, "Last used at");
    }

    public static UserSession restore(
            UUID id,
            UUID userId,
            String refreshTokenHash,
            String previousRefreshTokenHash,
            Instant refreshExpiresAt,
            Instant createdAt,
            Instant lastUsedAt,
            Instant revokedAt
    ) {
        return restore(
                id,
                userId,
                refreshTokenHash,
                previousRefreshTokenHash,
                refreshExpiresAt,
                createdAt,
                lastUsedAt,
                revokedAt,
                null,
                null
        );
    }

    public static UserSession restore(
            UUID id,
            UUID userId,
            String refreshTokenHash,
            String previousRefreshTokenHash,
            Instant refreshExpiresAt,
            Instant createdAt,
            Instant lastUsedAt,
            Instant revokedAt,
            String ipAddress,
            String userAgent
    ) {
        return new UserSession(
                id,
                userId,
                refreshTokenHash,
                previousRefreshTokenHash,
                refreshExpiresAt,
                createdAt,
                lastUsedAt,
                revokedAt,
                ipAddress,
                userAgent
        );
    }
}

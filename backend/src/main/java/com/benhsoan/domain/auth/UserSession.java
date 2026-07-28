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

    private UserSession(
            UUID id,
            UUID userId,
            String refreshTokenHash,
            String previousRefreshTokenHash,
            Instant refreshExpiresAt,
            Instant createdAt,
            Instant lastUsedAt,
            Instant revokedAt
    ) {
        this.id = Guard.require(id, "Session id");
        this.userId = Guard.require(userId, "User id");
        this.refreshTokenHash = Guard.require(refreshTokenHash, "Refresh token hash");
        this.previousRefreshTokenHash = previousRefreshTokenHash;
        this.refreshExpiresAt = Guard.require(refreshExpiresAt, "Refresh expires at");
        this.createdAt = Guard.require(createdAt, "Created at");

        this.lastUsedAt = lastUsedAt;
        this.revokedAt = revokedAt;
    }

    public static UserSession create(
            UUID userId,
            String refreshTokenHash,
            Instant refreshExpiresAt
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
                null
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
        return now.isAfter(lastUsedAt.plus(timeout));
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
        return new UserSession(
                id,
                userId,
                refreshTokenHash,
                previousRefreshTokenHash,
                refreshExpiresAt,
                createdAt,
                lastUsedAt,
                revokedAt
        );
    }
}

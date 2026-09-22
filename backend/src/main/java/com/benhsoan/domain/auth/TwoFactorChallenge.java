package com.benhsoan.domain.auth;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.shared.Guard.Guard;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString(exclude = "codeHash")
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TwoFactorChallenge {

    private UUID id;

    private UUID userId;

    private String codeHash;

    private Instant expiresAt;

    private int attempts;

    private Instant consumedAt;

    private Instant createdAt;

    private TwoFactorChallenge(
            UUID id,
            UUID userId,
            String codeHash,
            Instant expiresAt,
            int attempts,
            Instant consumedAt,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "Challenge id is required");
        this.userId = Objects.requireNonNull(userId, "User id is required");
        this.codeHash = Guard.require(codeHash, "Code hash");
        this.expiresAt = Objects.requireNonNull(expiresAt, "Expires at is required");
        this.attempts = attempts;
        this.consumedAt = consumedAt;
        this.createdAt = Objects.requireNonNull(createdAt, "Created at is required");
    }

    public static TwoFactorChallenge create(
            UUID userId,
            String codeHash,
            Instant expiresAt,
            Instant now
    ) {
        return new TwoFactorChallenge(
                UUID.randomUUID(),
                userId,
                codeHash,
                expiresAt,
                0,
                null,
                now
        );
    }

    public static TwoFactorChallenge restore(
            UUID id,
            UUID userId,
            String codeHash,
            Instant expiresAt,
            int attempts,
            Instant consumedAt,
            Instant createdAt
    ) {
        return new TwoFactorChallenge(
                id,
                userId,
                codeHash,
                expiresAt,
                attempts,
                consumedAt,
                createdAt
        );
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public boolean isValid(Instant now) {
        return !isExpired(now) && !isConsumed();
    }

    public void incrementAttempts() {
        this.attempts++;
    }

    public void markConsumed(Instant now) {
        this.consumedAt = Objects.requireNonNull(now, "Consumed at timestamp is required");
    }

    public void rotateCode(String newCodeHash, Instant newExpiresAt) {
        this.codeHash = Guard.require(newCodeHash, "Code hash");
        this.expiresAt = Objects.requireNonNull(newExpiresAt, "Expires at is required");
        this.attempts = 0;
    }
}

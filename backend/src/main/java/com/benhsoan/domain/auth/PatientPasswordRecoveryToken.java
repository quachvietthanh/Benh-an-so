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
public class PatientPasswordRecoveryToken {

    public static final int MAX_ATTEMPTS = 5;

    private UUID id;
    private UUID userId;
    private String phone;
    private String codeHash;
    private Instant expiresAt;
    private int attempts;
    private Instant usedAt;
    private Instant createdAt;

    private PatientPasswordRecoveryToken(
            UUID id,
            UUID userId,
            String phone,
            String codeHash,
            Instant expiresAt,
            int attempts,
            Instant usedAt,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "Token id is required");
        this.userId = Objects.requireNonNull(userId, "User id is required");
        this.phone = Guard.require(phone, "Phone");
        this.codeHash = Guard.require(codeHash, "Code hash");
        this.expiresAt = Objects.requireNonNull(expiresAt, "Expires at is required");
        this.attempts = attempts;
        this.usedAt = usedAt;
        this.createdAt = Objects.requireNonNull(createdAt, "Created at is required");
    }

    public static PatientPasswordRecoveryToken create(
            UUID userId,
            String phone,
            String codeHash,
            Instant expiresAt,
            Instant now
    ) {
        return new PatientPasswordRecoveryToken(
                UUID.randomUUID(),
                userId,
                phone,
                codeHash,
                expiresAt,
                0,
                null,
                now
        );
    }

    public static PatientPasswordRecoveryToken restore(
            UUID id,
            UUID userId,
            String phone,
            String codeHash,
            Instant expiresAt,
            int attempts,
            Instant usedAt,
            Instant createdAt
    ) {
        return new PatientPasswordRecoveryToken(
                id,
                userId,
                phone,
                codeHash,
                expiresAt,
                attempts,
                usedAt,
                createdAt
        );
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isAttemptsExceeded() {
        return attempts >= MAX_ATTEMPTS;
    }

    public boolean isValid(Instant now) {
        return !isExpired(now) && !isUsed() && !isAttemptsExceeded();
    }

    public void incrementAttempts() {
        this.attempts++;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public void markUsed(Instant now) {
        this.usedAt = Objects.requireNonNull(now, "Used at timestamp is required");
    }
}

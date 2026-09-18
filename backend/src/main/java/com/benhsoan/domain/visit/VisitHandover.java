package com.benhsoan.domain.visit;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.shared.Guard.Guard;
import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VisitHandover {

    private UUID id;
    private UUID visitId;
    private UUID fromDoctorId;
    private UUID toDoctorId;
    private String reason;
    private Instant handedOverAt;
    private UUID createdBy;
    private Instant createdAt;

    private VisitHandover(UUID id, UUID visitId, UUID fromDoctorId, UUID toDoctorId,
            String reason, Instant handedOverAt, UUID createdBy, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "Handover id is required.");
        this.visitId = Objects.requireNonNull(visitId, "Visit id is required.");
        this.fromDoctorId = Objects.requireNonNull(fromDoctorId, "From doctor id is required.");
        this.toDoctorId = Objects.requireNonNull(toDoctorId, "To doctor id is required.");
        if (fromDoctorId.equals(toDoctorId)) {
            throw new ValidationException("Cannot handover to the same doctor.");
        }
        this.reason = validateReason(reason);
        this.handedOverAt = Objects.requireNonNull(handedOverAt, "Handed over at is required.");
        this.createdBy = Objects.requireNonNull(createdBy, "Created by is required.");
        this.createdAt = Objects.requireNonNull(createdAt, "Created at is required.");
    }

    public static VisitHandover create(UUID visitId, UUID fromDoctorId, UUID toDoctorId,
            String reason, UUID createdBy, Instant at) {
        return new VisitHandover(UUID.randomUUID(), visitId, fromDoctorId, toDoctorId,
                reason, at, createdBy, at);
    }

    public static VisitHandover restore(UUID id, UUID visitId, UUID fromDoctorId, UUID toDoctorId,
            String reason, Instant handedOverAt, UUID createdBy, Instant createdAt) {
        return new VisitHandover(id, visitId, fromDoctorId, toDoctorId, reason,
                handedOverAt, createdBy, createdAt);
    }

    private static String validateReason(String reason) {
        String validated = reason == null ? null : reason.trim();
        if (validated == null || validated.isBlank()) {
            throw new ValidationException("Handover reason is required.");
        }
        if (validated.length() > 500) {
            throw new ValidationException("Handover reason must not exceed 500 characters.");
        }
        return validated;
    }
}

package com.benhsoan.domain.visit;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.shared.Guard.Guard;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.domain.visit.exception.VisitAlreadyCancelledException;
import com.benhsoan.domain.visit.exception.VisitAlreadyCompletedException;
import com.benhsoan.domain.visit.exception.VisitInvalidStatusException;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Visit {

    private UUID id;
    private String visitCode;
    private UUID patientId;
    private UUID doctorId;
    private UUID appointmentId;
    private UUID queueId;
    private VisitType visitType;
    private VisitStatus status;
    private Instant visitAt;
    private Instant startedAt;
    private Instant completedAt;
    private String reason;
    private String note;
    private UUID createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    private Visit(UUID id, String visitCode, UUID patientId, UUID doctorId, UUID appointmentId, UUID queueId,
            VisitType visitType, VisitStatus status, Instant visitAt, Instant startedAt, Instant completedAt,
            String reason, String note, UUID createdBy, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.visitCode = Guard.require(visitCode, "Visit code");
        this.patientId = Objects.requireNonNull(patientId);
        this.doctorId = Objects.requireNonNull(doctorId);
        this.appointmentId = appointmentId;
        this.queueId = queueId;
        this.visitType = Objects.requireNonNull(visitType);
        this.status = Objects.requireNonNull(status);
        this.visitAt = Objects.requireNonNull(visitAt);
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.reason = Guard.require(reason, "Reason");
        this.note = note;
        this.createdBy = Objects.requireNonNull(createdBy);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = updatedAt;
    }

    public static Visit create(String code, UUID patientId, UUID doctorId, UUID appointmentId, UUID queueId, VisitType type, Instant visitAt, String reason, String note, UUID createdBy) {
        return new Visit(UUID.randomUUID(), code, patientId, doctorId, appointmentId, queueId, type, VisitStatus.WAITING, visitAt, null, null, reason, note, createdBy, Instant.now(), null);
    }

    public static Visit restore(UUID id, String code, UUID patientId, UUID doctorId, UUID appointmentId, UUID queueId, VisitType type, VisitStatus status, Instant visitAt, Instant startedAt, Instant completedAt, String reason, String note, UUID createdBy, Instant createdAt, Instant updatedAt) {
        return new Visit(id, code, patientId, doctorId, appointmentId, queueId, type, status, visitAt, startedAt, completedAt, reason, note, createdBy, createdAt, updatedAt);
    }

    public void start(Instant at) {
        require(VisitStatus.WAITING);
        startedAt = Objects.requireNonNull(at);
        status = VisitStatus.IN_PROGRESS;
        updatedAt = at;
    }

    public void waitForResult(Instant at) {
        require(VisitStatus.IN_PROGRESS);
        status = VisitStatus.WAITING_FOR_RESULT;
        updatedAt = Objects.requireNonNull(at);
    }

    public void resume(Instant at) {
        require(VisitStatus.WAITING_FOR_RESULT);
        status = VisitStatus.IN_PROGRESS;
        updatedAt = Objects.requireNonNull(at);
    }

    public void complete(Instant at) {
        if (status != VisitStatus.IN_PROGRESS && status != VisitStatus.WAITING_FOR_RESULT) {
            if (status == VisitStatus.COMPLETED) throw new VisitAlreadyCompletedException();
            if (status == VisitStatus.CANCELLED) throw new VisitAlreadyCancelledException();
            throw new VisitInvalidStatusException("Only active visits can be completed.");
        
        }if (startedAt == null || at.isBefore(startedAt)) {
            throw new ValidationException("Completion time must not be before start time.");
        
        }status = VisitStatus.COMPLETED;
        completedAt = at;
        updatedAt = at;
    }

    public void cancel(Instant at) {
        if (status == VisitStatus.COMPLETED || status == VisitStatus.CANCELLED) {
            if (status == VisitStatus.COMPLETED) throw new VisitAlreadyCompletedException();
            throw new VisitAlreadyCancelledException();
        
        }status = VisitStatus.CANCELLED;
        updatedAt = Objects.requireNonNull(at);
    }

    public void updateRegistrationInformation(UUID doctorId, UUID appointmentId, UUID queueId, VisitType type, Instant visitAt, String reason, String note, Instant at) {
        require(VisitStatus.WAITING);
        this.doctorId = Objects.requireNonNull(doctorId);
        this.appointmentId = appointmentId;
        this.queueId = queueId;
        this.visitType = Objects.requireNonNull(type);
        this.visitAt = Objects.requireNonNull(visitAt);
        this.reason = Guard.require(reason, "Reason");
        this.note = note;
        this.updatedAt = Objects.requireNonNull(at);
    }

    public void updateNote(String note, Instant at) {
        if (status == VisitStatus.COMPLETED || status == VisitStatus.CANCELLED) {
            throw new VisitInvalidStatusException("Finished visits cannot be updated.");
        
        }this.note = note;
        this.updatedAt = Objects.requireNonNull(at);
    }

    public boolean isActive() {
        return status == VisitStatus.WAITING || status == VisitStatus.IN_PROGRESS || status == VisitStatus.WAITING_FOR_RESULT;
    }

    public boolean isCompleted() {
        return status == VisitStatus.COMPLETED;
    }

    public boolean isCancelled() {
        return status == VisitStatus.CANCELLED;
    }

    private void require(VisitStatus expected) {
        if (status != expected) {
            if (status == VisitStatus.COMPLETED) throw new VisitAlreadyCompletedException();
            if (status == VisitStatus.CANCELLED) throw new VisitAlreadyCancelledException();
            throw new VisitInvalidStatusException("Invalid visit status transition.");
    
        }}
}

package com.benhsoan.domain.queue;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.queue.enums.PriorityLevel;
import com.benhsoan.domain.queue.enums.QueueStatus;
import com.benhsoan.domain.queue.exception.InvalidStatusTransitionException;
import com.benhsoan.domain.shared.Guard.Guard;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicalQueue {

    private UUID id;

    private UUID patientId;

    private UUID doctorId;

    private String roomNumber;

    private int queueNumber;

    private QueueStatus status;

    private PriorityLevel priorityLevel;

    private String notes;

    private Instant checkedInAt;

    private Instant calledAt;

    private Instant startedAt;

    private Instant waitingForResultAt;

    private Instant completedAt;

    private Instant cancelledAt;

    private String cancelReason;

    private UUID createdBy;

    private Instant createdAt;

    private Instant updatedAt;


    private MedicalQueue(
            UUID id,
            UUID patientId,
            UUID doctorId,
            String roomNumber,
            int queueNumber,
            QueueStatus status,
            PriorityLevel priorityLevel,
            String notes,
            Instant checkedInAt,
            Instant calledAt,
            Instant startedAt,
            Instant waitingForResultAt,
            Instant completedAt,
            Instant cancelledAt,
            String cancelReason,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.patientId = Guard.require(patientId, "Patient id");
        this.queueNumber = queueNumber;
        this.status = Guard.require(status, "Status");
        this.priorityLevel = Guard.require(priorityLevel, "Priority level");
        this.createdBy = Guard.require(createdBy, "Created by");
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);

        this.doctorId = doctorId;
        this.roomNumber = roomNumber;
        this.notes = notes;
        this.checkedInAt = checkedInAt;
        this.calledAt = calledAt;
        this.startedAt = startedAt;
        this.waitingForResultAt = waitingForResultAt;
        this.completedAt = completedAt;
        this.cancelledAt = cancelledAt;
        this.cancelReason = cancelReason;
    }

    // ---- Factory ----

    public static MedicalQueue create(
            UUID patientId,
            int queueNumber,
            PriorityLevel priorityLevel,
            String roomNumber,
            UUID createdBy
    ) {
        return create(patientId, queueNumber, priorityLevel, roomNumber, null, createdBy);
    }

    public static MedicalQueue create(
            UUID patientId,
            int queueNumber,
            PriorityLevel priorityLevel,
            String roomNumber,
            UUID doctorId,
            UUID createdBy
    ) {
        Instant now = Instant.now();

        return new MedicalQueue(
                UUID.randomUUID(),
                patientId,
                doctorId,
                roomNumber,
                queueNumber,
                QueueStatus.WAITING,
                priorityLevel,
                null,
                now,
                null,
                null,
                null,
                null,
                null,
                null,
                createdBy,
                now,
                now
        );
    }

    // ---- State transitions ----

    public void call(UUID doctorId) {
        validateTransition(QueueStatus.IN_PROGRESS);
        this.doctorId = Guard.require(doctorId, "Doctor id");
        this.status = QueueStatus.IN_PROGRESS;
        this.calledAt = Instant.now();
        this.startedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void skip() {
        validateTransition(QueueStatus.SKIPPED);
        this.status = QueueStatus.SKIPPED;
        this.updatedAt = Instant.now();
    }

    public void resumeFromSkipped() {
        if (this.status != QueueStatus.SKIPPED) {
            throw new InvalidStatusTransitionException(
                    this.status, QueueStatus.IN_PROGRESS
            );
        }
        this.status = QueueStatus.IN_PROGRESS;
        this.calledAt = Instant.now();
        this.startedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void sendToWaitingForResult() {
        validateTransition(QueueStatus.WAITING_FOR_RESULT);
        this.status = QueueStatus.WAITING_FOR_RESULT;
        this.waitingForResultAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void resumeFromWaitingForResult() {
        if (this.status != QueueStatus.WAITING_FOR_RESULT) {
            throw new InvalidStatusTransitionException(
                    this.status, QueueStatus.IN_PROGRESS
            );
        }
        this.status = QueueStatus.IN_PROGRESS;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        validateTransition(QueueStatus.COMPLETED);
        this.status = QueueStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void cancel(String reason) {
        if (this.status == QueueStatus.COMPLETED) {
            throw new InvalidStatusTransitionException(
                    this.status, QueueStatus.CANCELLED
            );
        }
        this.status = QueueStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.cancelReason = reason;
        this.updatedAt = Instant.now();
    }

    // ---- Validation ----

    private void validateTransition(QueueStatus target) {
        switch (this.status) {
            case WAITING:
                if (target != QueueStatus.IN_PROGRESS
                        && target != QueueStatus.SKIPPED
                        && target != QueueStatus.CANCELLED) {
                    throw new InvalidStatusTransitionException(this.status, target);
                }
                break;
            case IN_PROGRESS:
                if (target != QueueStatus.WAITING_FOR_RESULT
                        && target != QueueStatus.COMPLETED
                        && target != QueueStatus.CANCELLED) {
                    throw new InvalidStatusTransitionException(this.status, target);
                }
                break;
            case WAITING_FOR_RESULT:
                if (target != QueueStatus.IN_PROGRESS
                        && target != QueueStatus.COMPLETED
                        && target != QueueStatus.CANCELLED) {
                    throw new InvalidStatusTransitionException(this.status, target);
                }
                break;
            case SKIPPED:
                if (target != QueueStatus.IN_PROGRESS
                        && target != QueueStatus.CANCELLED) {
                    throw new InvalidStatusTransitionException(this.status, target);
                }
                break;
            case COMPLETED:
            case CANCELLED:
                throw new InvalidStatusTransitionException(this.status, target);
        }
    }

    // ---- Restore ----

    public static MedicalQueue restore(
            UUID id,
            UUID patientId,
            UUID doctorId,
            String roomNumber,
            int queueNumber,
            QueueStatus status,
            PriorityLevel priorityLevel,
            String notes,
            Instant checkedInAt,
            Instant calledAt,
            Instant startedAt,
            Instant waitingForResultAt,
            Instant completedAt,
            Instant cancelledAt,
            String cancelReason,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt
    ) {
        MedicalQueue q = new MedicalQueue(
                id,
                patientId,
                doctorId,
                roomNumber,
                queueNumber,
                status,
                priorityLevel,
                notes,
                checkedInAt,
                calledAt,
                startedAt,
                waitingForResultAt,
                completedAt,
                cancelledAt,
                cancelReason,
                createdBy,
                createdAt,
                updatedAt
        );
        return q;
    }
}

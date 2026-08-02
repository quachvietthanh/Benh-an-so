package com.benhsoan.domain.queue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.queue.enums.MedicalQueueStatus;
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

    private UUID doctorId;
    private UUID roomId;
    private LocalDate queueDate;
    private MedicalQueueStatus status;

    private Instant createdAt;

    private Instant updatedAt;


    private MedicalQueue(
            UUID id,
            UUID doctorId,
            UUID roomId,
            LocalDate queueDate,
            MedicalQueueStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.doctorId = Guard.require(doctorId, "Doctor id");
        this.roomId = Guard.require(roomId, "Room id");
        this.queueDate = Guard.require(queueDate, "Queue date");
        this.status = Guard.require(status, "Status");
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);

    }

    public static MedicalQueue restore(UUID id, UUID doctorId, UUID roomId, LocalDate queueDate,
            MedicalQueueStatus status, Instant createdAt, Instant updatedAt) {
        return new MedicalQueue(id, doctorId, roomId, queueDate, status, createdAt, updatedAt);
    }

    public static MedicalQueue create(UUID doctorId, UUID roomId, LocalDate queueDate, Instant createdAt) {
        return new MedicalQueue(UUID.randomUUID(), doctorId, roomId, queueDate,
                MedicalQueueStatus.OPEN, createdAt, createdAt);
    }

    public void close(Instant closedAt) {
        if (status == MedicalQueueStatus.CLOSED) {
            return;
        }
        this.status = MedicalQueueStatus.CLOSED;
        this.updatedAt = Guard.require(closedAt, "Closed at");
    }
}

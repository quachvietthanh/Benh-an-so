package com.benhsoan.persistence.entity.queue;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "doctor_room_assignments")
@Getter
@Setter
public class DoctorRoomAssignmentEntity {
    @Id @Column(columnDefinition = "BINARY(16)") private UUID id;
    @Column(name = "doctor_id") private UUID doctorId;
    @Column(name = "room_id") private UUID roomId;
    @Column(name = "assigned_by") private UUID assignedBy;
    @Column(name = "assigned_at") private Instant assignedAt;
}

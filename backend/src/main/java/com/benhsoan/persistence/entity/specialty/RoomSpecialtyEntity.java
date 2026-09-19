package com.benhsoan.persistence.entity.specialty;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "room_specialties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomSpecialtyEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "room_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID roomId;

    @Column(name = "specialty_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID specialtyId;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;
}

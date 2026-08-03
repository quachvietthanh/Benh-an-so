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
@Table(name = "rooms")
@Getter
@Setter
public class RoomEntity {
    @Id @Column(nullable = false, columnDefinition = "BINARY(16)") private UUID id;
    @Column(name = "room_code", nullable = false, unique = true, length = 30) private String code;
    @Column(name = "room_name", nullable = false, length = 100) private String name;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;
}

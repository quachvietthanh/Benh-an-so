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
    @Id @Column(columnDefinition = "BINARY(16)") private UUID id;
    @Column(name = "room_code") private String code;
    @Column(name = "room_name") private String name;
    private boolean active;
    @Column(name = "created_at") private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;
}

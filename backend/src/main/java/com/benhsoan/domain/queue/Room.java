package com.benhsoan.domain.queue;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.shared.Guard.Guard;

import lombok.Getter;

@Getter
public class Room {
    private final UUID id;
    private final String code;
    private final String name;
    private final boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Room(UUID id, String code, String name, boolean active, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.code = Guard.require(code, "Room code");
        this.name = Guard.require(name, "Room name");
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Room restore(UUID id, String code, String name, boolean active, Instant createdAt, Instant updatedAt) {
        return new Room(id, code, name, active, createdAt, updatedAt);
    }
}

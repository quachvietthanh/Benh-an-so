package com.benhsoan.domain.queue;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import com.benhsoan.domain.shared.Guard.Guard;
import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.Getter;

@Getter
public class Room {

    private static final int MAX_CODE_LENGTH = 30;
    private static final int MAX_NAME_LENGTH = 100;

    private final UUID id;
    private final String code;
    private String name;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    private Room(UUID id, String code, String name, boolean active, Instant createdAt, Instant updatedAt) {
        this.id = Guard.require(id, "Room id");
        this.code = normalizeCode(code);
        this.name = normalizeName(name);
        this.active = active;
        this.createdAt = Guard.require(createdAt, "Created at");
        this.updatedAt = updatedAt;
    }

    public static Room create(String code, String name, Instant createdAt) {
        return new Room(UUID.randomUUID(), code, name, true, createdAt, null);
    }

    public static Room restore(UUID id, String code, String name, boolean active, Instant createdAt,
            Instant updatedAt) {
        return new Room(id, code, name, active, createdAt, updatedAt);
    }

    public void updateName(String name, Instant updatedAt) {
        this.name = normalizeName(name);
        this.updatedAt = Guard.require(updatedAt, "Updated at");
    }

    public void activate(Instant activatedAt) {
        this.active = true;
        this.updatedAt = Guard.require(activatedAt, "Activated at");
    }

    public void deactivate(Instant deactivatedAt) {
        this.active = false;
        this.updatedAt = Guard.require(deactivatedAt, "Deactivated at");
    }

    private static String normalizeCode(String code) {
        String normalizedCode = Guard.require(code, "Room code").trim().toUpperCase(Locale.ROOT);
        if (normalizedCode.length() > MAX_CODE_LENGTH) {
            throw new ValidationException("Room code must not exceed 30 characters.");
        }
        return normalizedCode;
    }

    private static String normalizeName(String name) {
        String normalizedName = Guard.require(name, "Room name").trim();
        if (normalizedName.length() > MAX_NAME_LENGTH) {
            throw new ValidationException("Room name must not exceed 100 characters.");
        }
        return normalizedName;
    }
}

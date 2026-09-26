package com.benhsoan.domain.specialty;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import com.benhsoan.domain.shared.Guard.Guard;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.specialty.exception.CannotDeactivateDefaultSpecialtyException;

import lombok.Getter;

@Getter
public class Specialty {

    public static final UUID GENERAL_ID = UUID.fromString("f0000000-0000-0000-0000-000000000001");

    private static final int MAX_CODE_LENGTH = 30;
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private final UUID id;
    private final String code;
    private String name;
    private String nameKey;
    private String description;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    private Specialty(UUID id, String code, String name, String description, boolean active, Instant createdAt, Instant updatedAt) {
        this.id = Guard.require(id, "Specialty id");
        this.code = normalizeCode(code);
        this.name = normalizeName(name);
        this.nameKey = toNameKey(this.name);
        this.description = normalizeDescription(description);
        this.active = active;
        this.createdAt = Guard.require(createdAt, "Created at");
        this.updatedAt = updatedAt;
    }

    public static Specialty create(String code, String name, String description, Instant now) {
        Instant effectiveNow = Guard.require(now, "Created at");
        return new Specialty(UUID.randomUUID(), code, name, description, true, effectiveNow, effectiveNow);
    }

    public static Specialty restore(UUID id, String code, String name, String description, boolean active, Instant createdAt, Instant updatedAt) {
        return new Specialty(id, code, name, description, active, createdAt, updatedAt);
    }

    public static Specialty restore(UUID id, String code, String name, boolean active, Instant createdAt, Instant updatedAt) {
        return new Specialty(id, code, name, null, active, createdAt, updatedAt);
    }

    public void update(String name, String description, Instant now) {
        this.name = normalizeName(name);
        this.nameKey = toNameKey(this.name);
        this.description = normalizeDescription(description);
        this.updatedAt = Guard.require(now, "Updated at");
    }

    public void deactivate(Instant now) {
        if (GENERAL_ID.equals(this.id)) {
            throw new CannotDeactivateDefaultSpecialtyException();
        }
        this.active = false;
        this.updatedAt = Guard.require(now, "Updated at");
    }

    public void activate(Instant now) {
        this.active = true;
        this.updatedAt = Guard.require(now, "Updated at");
    }

    public static String toNameKey(String name) {
        return Guard.require(name, "Specialty name").trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeCode(String code) {
        String normalized = Guard.require(code, "Specialty code").trim().toUpperCase(Locale.ROOT);
        if (normalized.length() > MAX_CODE_LENGTH) {
            throw new ValidationException("Specialty code must not exceed 30 characters.");
        }
        if (!normalized.matches("^[A-Z0-9_]+$")) {
            throw new ValidationException("Specialty code must only contain letters, numbers, and underscores.");
        }
        return normalized;
    }

    private static String normalizeName(String name) {
        String normalized = Guard.require(name, "Specialty name").trim();
        if (normalized.length() > MAX_NAME_LENGTH) {
            throw new ValidationException("Specialty name must not exceed 100 characters.");
        }
        return normalized;
    }

    private static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        String normalized = description.trim();
        if (normalized.length() > MAX_DESCRIPTION_LENGTH) {
            throw new ValidationException("Specialty description must not exceed 500 characters.");
        }
        return normalized;
    }
}

package com.benhsoan.domain.specialty;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import com.benhsoan.domain.shared.Guard.Guard;
import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.Getter;

@Getter
public class Specialty {

    public static final UUID GENERAL_ID = UUID.fromString("f0000000-0000-0000-0000-000000000001");

    private static final int MAX_CODE_LENGTH = 30;
    private static final int MAX_NAME_LENGTH = 100;

    private final UUID id;
    private final String code;
    private final String name;
    private final boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Specialty(UUID id, String code, String name, boolean active, Instant createdAt, Instant updatedAt) {
        this.id = Guard.require(id, "Specialty id");
        this.code = normalizeCode(code);
        this.name = normalizeName(name);
        this.active = active;
        this.createdAt = Guard.require(createdAt, "Created at");
        this.updatedAt = updatedAt;
    }

    public static Specialty restore(UUID id, String code, String name, boolean active, Instant createdAt, Instant updatedAt) {
        return new Specialty(id, code, name, active, createdAt, updatedAt);
    }

    private static String normalizeCode(String code) {
        String normalized = Guard.require(code, "Specialty code").trim().toUpperCase(Locale.ROOT);
        if (normalized.length() > MAX_CODE_LENGTH) {
            throw new ValidationException("Specialty code must not exceed 30 characters.");
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
}

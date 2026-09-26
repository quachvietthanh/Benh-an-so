package com.benhsoan.domain.auth;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.shared.Guard.Guard;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode(of = "code")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Permission {

    private UUID id;
    private String code;
    private String name;
    private String module;
    private String description;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    private Permission(UUID id, String code, String name, String module, String description,
            boolean active, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.code = Guard.require(code, "Permission code");
        this.name = Guard.require(name, "Permission name");
        this.module = Guard.require(module, "Permission module");
        this.description = description;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Permission restore(UUID id, String code, String name, String module, String description,
            boolean active, Instant createdAt, Instant updatedAt) {
        return new Permission(Objects.requireNonNull(id), code, name, module, description, active,
                Objects.requireNonNull(createdAt), updatedAt);
    }

    /** Temporary factory for role construction before a persisted permission is available. */
    public static Permission fromCode(String code) {
        return new Permission(null, code, code, "UNSPECIFIED", null, true, null, null);
    }
}

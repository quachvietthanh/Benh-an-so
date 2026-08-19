package com.benhsoan.domain.auth;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.benhsoan.domain.shared.Guard.Guard;
import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Role {

    private UUID id;

    private String name;

    private String description;

    private boolean system;

    private Instant createdAt;

    private Instant updatedAt;

    private final Set<Permission> permissions = new HashSet<>();

    private Role(
            UUID id,
            String name,
            String description,
            boolean system,
            Instant createdAt,
            Instant updatedAt,
            Set<?> permissions
    ) {
        this.id = Objects.requireNonNull(id);
        this.name = Guard.require(name, "Role name");
        this.description = description;
        this.system = system;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = updatedAt;

        if (permissions != null) {
            permissions.stream()
                    .map(Role::normalizePermission)
                    .forEach(this.permissions::add);
        }
    }

    public static Role create(
            String name,
            String description,
            boolean system,
            Set<?> permissions
    ) {
        Instant now = Instant.now();

        return new Role(
                UUID.randomUUID(),
                name,
                description,
                system,
                now,
                now,
                permissions
        );
    }

    public static Role restore(
            UUID id,
            String name,
            String description,
            boolean system,
            Instant createdAt,
            Instant updatedAt,
            Set<?> permissions
    ) {
        return new Role(
                id,
                name,
                description,
                system,
                createdAt,
                updatedAt,
                permissions
        );
    }

    public void rename(String name) {
        if (system) {
            throw new ValidationException("System role cannot be renamed.");
        }

        this.name = Guard.require(name, "Role name");
        this.updatedAt = Instant.now();
    }

    public void changeDescription(String description) {
        this.description = description;
        this.updatedAt = Instant.now();
    }

    public void addPermission(Object permission) {
        Permission normalized = normalizePermission(permission);

        if (permissions.add(normalized)) {
            updatedAt = Instant.now();
        }
    }

    public void removePermission(Object permission) {
        if (permissions.remove(normalizePermission(permission))) {
            updatedAt = Instant.now();
        }
    }

    public boolean hasPermission(Object permission) {
        return permissions.contains(normalizePermission(permission));
    }

    public boolean hasAnyPermission(Object... permissions) {
        return Arrays.stream(permissions)
                .map(Role::normalizePermission)
                .anyMatch(this.permissions::contains);
    }

    public boolean hasAllPermissions(Object... permissions) {
        return Arrays.stream(permissions)
                .map(Role::normalizePermission)
                .allMatch(this.permissions::contains);
    }

    public Set<Permission> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }

    public void replacePermissions(Set<Permission> permissions) {
        this.permissions.clear();
        this.permissions.addAll(permissions);
        this.updatedAt = Instant.now();
    }

    private static Permission normalizePermission(Object permission) {
        Objects.requireNonNull(permission);
        if (permission instanceof Permission value) return value;
        if (permission instanceof Enum<?> value) return Permission.fromCode(value.name());
        if (permission instanceof String value) return Permission.fromCode(value);
        throw new IllegalArgumentException("Unsupported permission type: " + permission.getClass().getName());
    }
}

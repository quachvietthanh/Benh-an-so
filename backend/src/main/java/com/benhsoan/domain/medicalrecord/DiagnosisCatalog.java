package com.benhsoan.domain.medicalrecord;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.shared.Guard.Guard;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiagnosisCatalog {

    private UUID id;
    private String code, name, diseaseGroup, description;
    private boolean active;
    private Instant createdAt, updatedAt;

    private DiagnosisCatalog(
            UUID id,
            String code,
            String name,
            String diseaseGroup,
            String description,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.code = normalizeCode(code);
        this.name = Guard.require(name, "Diagnosis name");
        this.diseaseGroup = Guard.require(diseaseGroup, "Disease group");
        this.description = description;
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = updatedAt;
    }

    public static DiagnosisCatalog create(String code, String name, String diseaseGroup, String description) {
        return new DiagnosisCatalog(UUID.randomUUID(), code, name, diseaseGroup, description, true, Instant.now(), null);
    }

    public static DiagnosisCatalog create(
            UUID id,
            String code,
            String name,
            String diseaseGroup,
            String description,
            Instant createdAt
    ) {
        return new DiagnosisCatalog(id, code, name, diseaseGroup, description, true, createdAt, null);
    }

    public static DiagnosisCatalog restore(
            UUID id,
            String code,
            String name,
            String diseaseGroup,
            String description,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new DiagnosisCatalog(id, code, name, diseaseGroup, description, active, createdAt, updatedAt);
    }

    public void activate(Instant at) {
        active = true;
        updatedAt = Objects.requireNonNull(at);
    }

    public void deactivate(Instant at) {
        active = false;
        updatedAt = Objects.requireNonNull(at);
    }

    public void updateInformation(String name, String diseaseGroup, String description, Instant at) {
        this.name = Guard.require(name, "Diagnosis name");
        this.diseaseGroup = Guard.require(diseaseGroup, "Disease group");
        this.description = description;
        updatedAt = Objects.requireNonNull(at);
    }

    private static String normalizeCode(String code) {
        return Guard.require(code, "Diagnosis code").trim().toUpperCase(Locale.ROOT);
    }
}

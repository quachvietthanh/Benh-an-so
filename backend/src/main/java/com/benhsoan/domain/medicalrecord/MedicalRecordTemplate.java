package com.benhsoan.domain.medicalrecord;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateDefaultReplacementRequiredException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateInactiveException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateLastActiveException;
import com.benhsoan.domain.shared.Guard.Guard;
import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.Getter;

@Getter
public class MedicalRecordTemplate {

    private static final int MAX_NAME_LENGTH = 150;

    private final UUID id;
    private final UUID specialtyId;
    private String name;
    private String nameKey;
    private boolean active;
    private boolean defaultTemplate;
    private int currentVersionNo;
    private final UUID createdBy;
    private final Instant createdAt;
    private UUID updatedBy;
    private Instant updatedAt;
    private final List<MedicalRecordTemplateVersion> versions;

    private MedicalRecordTemplate(UUID id, UUID specialtyId, String name, boolean active, boolean defaultTemplate,
            int currentVersionNo, UUID createdBy, Instant createdAt, UUID updatedBy, Instant updatedAt,
            List<MedicalRecordTemplateVersion> versions) {
        this.id = Objects.requireNonNull(id);
        this.specialtyId = Objects.requireNonNull(specialtyId);
        this.name = normalizeName(name);
        this.nameKey = toNameKey(this.name);
        this.active = active;
        this.defaultTemplate = defaultTemplate;
        if (currentVersionNo < 1) {
            throw new ValidationException("Template current version must be positive.");
        }
        this.currentVersionNo = currentVersionNo;
        this.createdBy = Objects.requireNonNull(createdBy);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
        this.versions = normalizeVersions(versions, id, currentVersionNo);
    }

    public static MedicalRecordTemplate create(UUID specialtyId, String name, boolean makeDefault,
            List<MedicalRecordTemplateVersion.SectionDefinition> sections, UUID createdBy, Instant createdAt) {
        UUID templateId = UUID.randomUUID();
        String normalizedName = normalizeName(name);
        MedicalRecordTemplateVersion firstVersion = MedicalRecordTemplateVersion.create(templateId, 1, specialtyId,
                normalizedName, null, createdBy, createdAt, sections);
        return new MedicalRecordTemplate(templateId, specialtyId, normalizedName, true, makeDefault, 1, createdBy,
                createdAt, null, null, List.of(firstVersion));
    }

    public static MedicalRecordTemplate restore(UUID id, UUID specialtyId, String name, boolean active,
            boolean defaultTemplate, int currentVersionNo, UUID createdBy, Instant createdAt, UUID updatedBy,
            Instant updatedAt, List<MedicalRecordTemplateVersion> versions) {
        return new MedicalRecordTemplate(id, specialtyId, name, active, defaultTemplate, currentVersionNo, createdBy,
                createdAt, updatedBy, updatedAt, versions);
    }

    public void update(String name, List<MedicalRecordTemplateVersion.SectionDefinition> sections, String changeNote,
            UUID updatedBy, Instant updatedAt) {
        String normalizedName = normalizeName(name);
        int nextVersionNo = currentVersionNo + 1;
        MedicalRecordTemplateVersion nextVersion = MedicalRecordTemplateVersion.create(id, nextVersionNo, specialtyId,
                normalizedName, changeNote, updatedBy, updatedAt, sections);
        versions.add(nextVersion);
        this.name = normalizedName;
        this.nameKey = toNameKey(normalizedName);
        this.currentVersionNo = nextVersionNo;
        this.updatedBy = Objects.requireNonNull(updatedBy);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public void setDefault(UUID updatedBy, Instant updatedAt) {
        if (!active) {
            throw new MedicalRecordTemplateInactiveException();
        }
        this.defaultTemplate = true;
        this.updatedBy = Objects.requireNonNull(updatedBy);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public void clearDefault(UUID updatedBy, Instant updatedAt) {
        this.defaultTemplate = false;
        this.updatedBy = Objects.requireNonNull(updatedBy);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public void activate(UUID updatedBy, Instant updatedAt) {
        this.active = true;
        this.updatedBy = Objects.requireNonNull(updatedBy);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public void deactivate(int activeTemplateCount, UUID replacementTemplateId, UUID updatedBy, Instant updatedAt) {
        if (!active) {
            return;
        }
        if (activeTemplateCount <= 1) {
            throw new MedicalRecordTemplateLastActiveException();
        }
        if (defaultTemplate && (replacementTemplateId == null || replacementTemplateId.equals(id))) {
            throw new MedicalRecordTemplateDefaultReplacementRequiredException();
        }
        this.active = false;
        this.defaultTemplate = false;
        this.updatedBy = Objects.requireNonNull(updatedBy);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public MedicalRecordTemplateVersion getCurrentVersion() {
        return versions.getLast();
    }

    public List<MedicalRecordTemplateVersion> getVersions() {
        return List.copyOf(versions);
    }

    private static List<MedicalRecordTemplateVersion> normalizeVersions(List<MedicalRecordTemplateVersion> versions,
            UUID templateId, int currentVersionNo) {
        if (versions == null || versions.isEmpty()) {
            throw new ValidationException("Medical record template must have a version.");
        }
        List<MedicalRecordTemplateVersion> sorted = versions.stream()
                .sorted(Comparator.comparingInt(MedicalRecordTemplateVersion::getVersionNo))
                .toList();
        if (sorted.stream().anyMatch(version -> !templateId.equals(version.getTemplateId()))
                || sorted.getLast().getVersionNo() != currentVersionNo) {
            throw new ValidationException("Template versions are inconsistent with the template.");
        }
        return new java.util.ArrayList<>(sorted);
    }

    private static String normalizeName(String name) {
        String normalized = Guard.require(name, "Template name").trim();
        if (normalized.length() > MAX_NAME_LENGTH) {
            throw new ValidationException("Template name must not exceed 150 characters.");
        }
        return normalized;
    }

    private static String toNameKey(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}

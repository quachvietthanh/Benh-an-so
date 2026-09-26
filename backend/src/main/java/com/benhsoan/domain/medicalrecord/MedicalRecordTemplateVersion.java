package com.benhsoan.domain.medicalrecord;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.enums.MedicalRecordFieldCode;
import com.benhsoan.domain.shared.Guard.Guard;
import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.Getter;

@Getter
public class MedicalRecordTemplateVersion {

    private static final int MAX_CHANGE_NOTE_LENGTH = 500;

    private final UUID id;
    private final UUID templateId;
    private final int versionNo;
    private final UUID specialtyId;
    private final String templateName;
    private final String changeNote;
    private final UUID createdBy;
    private final Instant createdAt;
    private final List<MedicalRecordTemplateSection> sections;

    private MedicalRecordTemplateVersion(UUID id, UUID templateId, int versionNo, UUID specialtyId, String templateName,
            String changeNote, UUID createdBy, Instant createdAt, List<MedicalRecordTemplateSection> sections) {
        this.id = Objects.requireNonNull(id);
        this.templateId = Objects.requireNonNull(templateId);
        if (versionNo < 1) {
            throw new ValidationException("Template version number must be positive.");
        }
        this.versionNo = versionNo;
        this.specialtyId = Objects.requireNonNull(specialtyId);
        this.templateName = Guard.require(templateName, "Template name").trim();
        this.changeNote = normalizeChangeNote(changeNote);
        this.createdBy = Objects.requireNonNull(createdBy);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.sections = normalizeSections(sections);
        if (this.sections.stream().anyMatch(section -> !id.equals(section.getTemplateVersionId()))) {
            throw new ValidationException("Template sections must belong to this version.");
        }
    }

    public static MedicalRecordTemplateVersion create(UUID templateId, int versionNo, UUID specialtyId, String templateName,
            String changeNote, UUID createdBy, Instant createdAt, List<SectionDefinition> sectionDefinitions) {
        if (sectionDefinitions == null || sectionDefinitions.isEmpty()) {
            throw new ValidationException("Medical record template must contain at least one section.");
        }
        UUID versionId = UUID.randomUUID();
        List<MedicalRecordTemplateSection> sections = sectionDefinitions.stream()
                .map(section -> MedicalRecordTemplateSection.create(versionId, section.fieldCode(), section.label(),
                        section.required(), section.displayOrder()))
                .toList();
        return new MedicalRecordTemplateVersion(versionId, templateId, versionNo, specialtyId, templateName, changeNote,
                createdBy, createdAt, sections);
    }

    public static MedicalRecordTemplateVersion restore(UUID id, UUID templateId, int versionNo, UUID specialtyId,
            String templateName, String changeNote, UUID createdBy, Instant createdAt,
            List<MedicalRecordTemplateSection> sections) {
        return new MedicalRecordTemplateVersion(id, templateId, versionNo, specialtyId, templateName, changeNote,
                createdBy, createdAt, sections);
    }

    private static List<MedicalRecordTemplateSection> normalizeSections(List<MedicalRecordTemplateSection> sections) {
        if (sections == null || sections.isEmpty()) {
            throw new ValidationException("Medical record template must contain at least one section.");
        }
        Set<MedicalRecordFieldCode> fieldCodes = new HashSet<>();
        Set<Integer> displayOrders = new HashSet<>();
        for (MedicalRecordTemplateSection section : sections) {
            if (!fieldCodes.add(section.getFieldCode())) {
                throw new ValidationException("Template sections must not repeat field code.");
            }
            if (!displayOrders.add(section.getDisplayOrder())) {
                throw new ValidationException("Template sections must not repeat display order.");
            }
        }
        return sections.stream().sorted(Comparator.comparingInt(MedicalRecordTemplateSection::getDisplayOrder)).toList();
    }

    private static String normalizeChangeNote(String changeNote) {
        if (changeNote == null || changeNote.isBlank()) {
            return null;
        }
        String normalized = changeNote.trim();
        if (normalized.length() > MAX_CHANGE_NOTE_LENGTH) {
            throw new ValidationException("Template change note must not exceed 500 characters.");
        }
        return normalized;
    }

    public record SectionDefinition(MedicalRecordFieldCode fieldCode, String label, boolean required, int displayOrder) {
    }
}

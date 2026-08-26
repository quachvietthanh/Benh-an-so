package com.benhsoan.domain.medicalrecord;

import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.enums.MedicalRecordFieldCode;
import com.benhsoan.domain.shared.Guard.Guard;
import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.Getter;

@Getter
public class MedicalRecordTemplateSection {

    private static final int MAX_LABEL_LENGTH = 150;

    private final UUID id;
    private final UUID templateVersionId;
    private final MedicalRecordFieldCode fieldCode;
    private final String label;
    private final boolean required;
    private final int displayOrder;

    private MedicalRecordTemplateSection(UUID id, UUID templateVersionId, MedicalRecordFieldCode fieldCode,
            String label, boolean required, int displayOrder) {
        this.id = Objects.requireNonNull(id);
        this.templateVersionId = Objects.requireNonNull(templateVersionId);
        this.fieldCode = Objects.requireNonNull(fieldCode);
        this.label = normalizeLabel(label);
        this.required = required;
        if (displayOrder < 1) {
            throw new ValidationException("Template section display order must be positive.");
        }
        this.displayOrder = displayOrder;
    }

    public static MedicalRecordTemplateSection create(UUID templateVersionId, MedicalRecordFieldCode fieldCode,
            String label, boolean required, int displayOrder) {
        return new MedicalRecordTemplateSection(UUID.randomUUID(), templateVersionId, fieldCode, label, required,
                displayOrder);
    }

    public static MedicalRecordTemplateSection restore(UUID id, UUID templateVersionId, MedicalRecordFieldCode fieldCode,
            String label, boolean required, int displayOrder) {
        return new MedicalRecordTemplateSection(id, templateVersionId, fieldCode, label, required, displayOrder);
    }

    private static String normalizeLabel(String label) {
        String normalized = Guard.require(label, "Template section label").trim();
        if (normalized.length() > MAX_LABEL_LENGTH) {
            throw new ValidationException("Template section label must not exceed 150 characters.");
        }
        return normalized;
    }
}

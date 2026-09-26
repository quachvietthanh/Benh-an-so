package com.benhsoan.domain.prescription;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * NCL-05-CN-008: a doctor-scoped prescription template associated with a diagnosis
 * catalog entry (NCL-13-CN-002). It is an immutable snapshot of the instruction fields
 * needed to pre-populate a future prescription draft.
 */
@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PrescriptionTemplate {

    private UUID id;
    private UUID diagnosisCatalogId;
    private UUID createdBy;
    private Instant createdAt;
    private List<PrescriptionTemplateItem> items;

    private PrescriptionTemplate(
            UUID id,
            UUID diagnosisCatalogId,
            UUID createdBy,
            Instant createdAt,
            List<PrescriptionTemplateItem> items
    ) {
        this.id = requireNonNull(id, "Template id is required.");
        this.diagnosisCatalogId = requireNonNull(diagnosisCatalogId, "Diagnosis catalog id is required.");
        this.createdBy = requireNonNull(createdBy, "Creator id is required.");
        this.createdAt = requireNonNull(createdAt, "Creation time is required.");
        this.items = validateAndCopyItems(items, id);
    }

    public static PrescriptionTemplate create(
            UUID id,
            UUID diagnosisCatalogId,
            UUID createdBy,
            Instant createdAt,
            List<PrescriptionTemplateItem> items
    ) {
        return new PrescriptionTemplate(
                id,
                diagnosisCatalogId,
                createdBy,
                createdAt,
                items
        );
    }

    public static PrescriptionTemplate restore(
            UUID id,
            UUID diagnosisCatalogId,
            UUID createdBy,
            Instant createdAt,
            List<PrescriptionTemplateItem> items
    ) {
        return new PrescriptionTemplate(
                id,
                diagnosisCatalogId,
                createdBy,
                createdAt,
                items
        );
    }

    private static List<PrescriptionTemplateItem> validateAndCopyItems(
            List<PrescriptionTemplateItem> items,
            UUID templateId
    ) {
        if (items == null || items.isEmpty()) {
            throw new ValidationException("A prescription template must contain at least one medicine.");
        }

        Set<UUID> medicineIds = new HashSet<>();
        for (PrescriptionTemplateItem item : items) {
            if (item == null) {
                throw new ValidationException("Template item is required.");
            }
            if (!templateId.equals(item.getTemplateId())) {
                throw new ValidationException("Template item does not belong to template: " + templateId);
            }
            if (!medicineIds.add(item.getMedicineId())) {
                throw new ValidationException("Medicine already exists in the template: " + item.getMedicineId());
            }
        }

        return items.stream()
                .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                .toList();
    }

    private static <T> T requireNonNull(T value, String message) {
        if (Objects.isNull(value)) {
            throw new ValidationException(message);
        }
        return value;
    }
}

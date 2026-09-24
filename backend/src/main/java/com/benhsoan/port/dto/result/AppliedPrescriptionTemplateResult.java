package com.benhsoan.port.dto.result;

import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;

/**
 * NCL-05-CN-008: the result of applying a prescription template. It represents a
 * pre-populated draft only; no prescription is persisted. Skipped items correspond to
 * template medicines that are no longer active in the catalog (TC-03). Safety warnings
 * come from the existing check services, so the draft never bypasses medical validation.
 */
public record AppliedPrescriptionTemplateResult(
        UUID templateId,
        String diagnosisCode,
        String diagnosisName,
        List<DraftItem> items,
        List<SkippedItem> skippedItems,
        List<DrugInteractionWarningResult> interactionWarnings,
        List<PatientAllergyWarningResult> allergyWarnings,
        List<ContraindicationWarningResult> contraindicationWarnings,
        List<ContraindicationMissingDataResult> contraindicationMissingData
) {
    public record DraftItem(
            UUID medicineId,
            String medicineCode,
            String medicineName,
            String activeIngredient,
            String strength,
            String unit,
            String dosage,
            Integer frequency,
            AdministrationRoute route,
            Integer durationDays,
            int quantity,
            String instructions
    ) {
    }

    public record SkippedItem(
            UUID medicineId,
            String medicineName,
            String reason
    ) {
    }
}

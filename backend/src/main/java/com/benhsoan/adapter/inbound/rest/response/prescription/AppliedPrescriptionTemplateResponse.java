package com.benhsoan.adapter.inbound.rest.response.prescription;

import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;

public record AppliedPrescriptionTemplateResponse(
        UUID templateId,
        String diagnosisCode,
        String diagnosisName,
        List<DraftItem> items,
        List<SkippedItem> skippedItems,
        List<DrugInteractionWarningResponse> interactionWarnings,
        List<PatientAllergyWarningResponse> allergyWarnings,
        List<ContraindicationWarningResponse> contraindicationWarnings,
        List<ContraindicationMissingDataResponse> contraindicationMissingData
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

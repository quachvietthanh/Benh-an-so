package com.benhsoan.application.ucservice.prescription.snapshot;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionItem;

@Component
public class PrescriptionSnapshotMapper {

    private static final int SCHEMA_VERSION = 1;

    public PrescriptionSnapshot toSnapshot(Prescription prescription) {
        return new PrescriptionSnapshot(
                SCHEMA_VERSION,
                prescription.getId(),
                prescription.getPrescriptionCode(),
                prescription.getMedicalRecordId(),
                prescription.getStatus(),
                prescription.getNote(),
                prescription.getPrescribedBy(),
                prescription.getPrescribedAt(),
                prescription.getUpdatedBy(),
                prescription.getUpdatedAt(),
                sortedItems(prescription).stream()
                        .map(this::toSnapshotItem)
                        .toList()
        );
    }

    public PrescriptionBusinessState toBusinessState(
            Prescription prescription
    ) {
        return new PrescriptionBusinessState(
                normalizeOptionalText(prescription.getNote()),
                sortedItems(prescription).stream()
                        .map(this::toBusinessItem)
                        .toList()
        );
    }

    private List<PrescriptionItem> sortedItems(Prescription prescription) {
        return prescription.getItems().stream()
                .sorted(Comparator.comparing(PrescriptionItem::getMedicineId))
                .toList();
    }

    private PrescriptionSnapshot.Item toSnapshotItem(
            PrescriptionItem item
    ) {
        return new PrescriptionSnapshot.Item(
                item.getId(),
                item.getMedicineId(),
                item.getMedicineName(),
                item.getActiveIngredient(),
                item.getStrength(),
                item.getUnit(),
                item.getDosage(),
                item.getFrequency(),
                item.getRoute(),
                item.getDurationDays(),
                item.getQuantity(),
                item.getInstructions(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private PrescriptionBusinessState.Item toBusinessItem(
            PrescriptionItem item
    ) {
        return new PrescriptionBusinessState.Item(
                item.getMedicineId(),
                item.getMedicineName(),
                item.getActiveIngredient(),
                item.getStrength(),
                item.getUnit(),
                item.getDosage(),
                item.getFrequency(),
                item.getRoute(),
                item.getDurationDays(),
                item.getQuantity(),
                item.getInstructions()
        );
    }

    private String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

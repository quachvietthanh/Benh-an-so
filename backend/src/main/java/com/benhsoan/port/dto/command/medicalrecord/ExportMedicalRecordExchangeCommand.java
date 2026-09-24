package com.benhsoan.port.dto.command.medicalrecord;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.shared.exception.ValidationException;

/**
 * Command for exporting medical records according to data exchange structure (NCL-11-CN-007).
 */
public record ExportMedicalRecordExchangeCommand(
        List<UUID> medicalRecordIds,
        List<UUID> visitIds,
        String format
) {
    public ExportMedicalRecordExchangeCommand {
        boolean hasRecords = medicalRecordIds != null && !medicalRecordIds.isEmpty();
        boolean hasVisits = visitIds != null && !visitIds.isEmpty();
        if (!hasRecords && !hasVisits) {
            throw new ValidationException("At least one medical record ID or visit ID must be specified.");
        }
        medicalRecordIds = medicalRecordIds == null ? List.of() : List.copyOf(medicalRecordIds);
        visitIds = visitIds == null ? List.of() : List.copyOf(visitIds);
        format = (format == null || format.isBlank()) ? "JSON" : format.trim().toUpperCase();
        if (!"JSON".equals(format)) {
            throw new ValidationException("Chỉ hỗ trợ định dạng JSON cho việc xuất hồ sơ trao đổi dữ liệu y tế.");
        }
    }

    public static ExportMedicalRecordExchangeCommand forRecords(List<UUID> medicalRecordIds) {
        return new ExportMedicalRecordExchangeCommand(medicalRecordIds, null, "JSON");
    }

    public static ExportMedicalRecordExchangeCommand forVisits(List<UUID> visitIds) {
        return new ExportMedicalRecordExchangeCommand(null, visitIds, "JSON");
    }

    public static ExportMedicalRecordExchangeCommand forSingleRecord(UUID medicalRecordId) {
        return new ExportMedicalRecordExchangeCommand(List.of(Objects.requireNonNull(medicalRecordId)), null, "JSON");
    }
}

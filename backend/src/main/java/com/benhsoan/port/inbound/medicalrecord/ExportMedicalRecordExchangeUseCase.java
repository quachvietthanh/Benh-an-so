package com.benhsoan.port.inbound.medicalrecord;

import java.util.UUID;

import com.benhsoan.port.dto.command.medicalrecord.ExportMedicalRecordExchangeCommand;
import com.benhsoan.port.dto.result.MedicalRecordExchangeExportResult;

/**
 * Inbound use case for exporting medical records according to standard data exchange structure (NCL-11-CN-007).
 */
public interface ExportMedicalRecordExchangeUseCase {

    /**
     * Export one or multiple medical records into a standard data exchange file (JSON format).
     *
     * @param command export command containing medicalRecordIds or visitIds
     * @return export result containing downloadable content bytes and filename
     */
    MedicalRecordExchangeExportResult exportRecords(ExportMedicalRecordExchangeCommand command);

    /**
     * Export a single medical record into a standard data exchange file (JSON format).
     *
     * @param medicalRecordId the ID of the medical record
     * @return export result containing downloadable content bytes and filename
     */
    MedicalRecordExchangeExportResult exportSingleRecord(UUID medicalRecordId);
}

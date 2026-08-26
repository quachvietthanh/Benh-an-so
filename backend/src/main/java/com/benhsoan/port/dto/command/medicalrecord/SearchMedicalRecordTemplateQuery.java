package com.benhsoan.port.dto.command.medicalrecord;

import java.util.UUID;

public record SearchMedicalRecordTemplateQuery(UUID specialtyId, Boolean active) {
}

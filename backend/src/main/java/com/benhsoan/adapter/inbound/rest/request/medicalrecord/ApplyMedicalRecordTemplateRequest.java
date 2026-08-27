package com.benhsoan.adapter.inbound.rest.request.medicalrecord;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ApplyMedicalRecordTemplateRequest(@NotNull UUID templateId) {
}

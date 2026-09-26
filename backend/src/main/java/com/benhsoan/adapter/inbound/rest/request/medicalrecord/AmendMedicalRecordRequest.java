package com.benhsoan.adapter.inbound.rest.request.medicalrecord;

import jakarta.validation.constraints.NotBlank;

public record AmendMedicalRecordRequest(@NotBlank String content, @NotBlank String reason) {
}

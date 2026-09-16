package com.benhsoan.port.dto.command.medicalrecord;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

public record GetOverdueMedicalRecordsQuery(
        UUID doctorId,
        Pageable pageable
) {
}

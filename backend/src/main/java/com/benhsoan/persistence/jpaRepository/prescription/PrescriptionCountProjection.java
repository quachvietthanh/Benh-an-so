package com.benhsoan.persistence.jpaRepository.prescription;

import java.util.UUID;

public record PrescriptionCountProjection(
        UUID medicalRecordId,
        Long count
) {
}

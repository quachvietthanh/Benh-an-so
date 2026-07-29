package com.benhsoan.persistence.jpaRepository.patient;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;

public record PatientMedicalHistoryProjection(
        UUID visitId,
        String visitCode,
        VisitType visitType,
        VisitStatus visitStatus,
        Instant visitAt,
        Instant startedAt,
        Instant completedAt,
        String reason,
        String note,
        UUID doctorId,
        String doctorName,
        UUID medicalRecordId,
        MedicalRecordStatus medicalRecordStatus,
        String chiefComplaint,
        String conclusion
) {
}

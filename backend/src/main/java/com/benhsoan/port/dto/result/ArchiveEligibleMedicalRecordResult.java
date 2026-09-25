package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveEligibleMedicalRecordResult {
    private UUID medicalRecordId;
    private UUID visitId;
    private String visitCode;
    private UUID patientId;
    private String patientCode;
    private String patientFullName;
    private UUID doctorId;
    private String doctorFullName;
    private String specialtyName;
    private String status;
    private Instant completedAt;
    private Instant signedAt;
}

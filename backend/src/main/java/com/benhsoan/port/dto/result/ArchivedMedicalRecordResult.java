package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.time.LocalDate;
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
public class ArchivedMedicalRecordResult {
    private UUID medicalRecordId;
    private UUID visitId;
    private String visitCode;
    private UUID patientId;
    private String patientCode;
    private String patientFullName;
    private String patientPhone;
    private UUID doctorId;
    private String doctorFullName;
    private String conclusion;
    private LocalDate revisitDate;
    private String status;
    private Instant completedAt;
    private Instant signedAt;
    private Instant archivedAt;
    private UUID archivedBy;
}

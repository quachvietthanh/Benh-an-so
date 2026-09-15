package com.benhsoan.port.dto.result.patient;

import java.time.LocalDate;
import java.util.List;

import com.benhsoan.port.dto.result.PatientResult;

import lombok.Builder;

@Builder
public record DuplicatePatientGroupResult(
        String fullName,
        LocalDate dateOfBirth,
        String phone,
        List<PatientResult> candidates
) {
}

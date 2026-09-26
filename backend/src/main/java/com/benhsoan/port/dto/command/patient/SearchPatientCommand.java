package com.benhsoan.port.dto.command.patient;

import java.time.LocalDate;

import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.patient.enums.Gender;

import lombok.Builder;

@Builder
public record SearchPatientCommand(

        String patientCode,

        String fullName,

        String phone,

        String identityNumber,

        String insuranceNumber,

        LocalDate dateOfBirth,

        Gender gender,

        Boolean active,

        Pageable pageable

) {
}
package com.benhsoan.adapter.inbound.rest.request.patient;

import java.time.LocalDate;

import com.benhsoan.domain.patient.enums.Gender;

public record SearchPatientRequest(

        String patientCode,

        String fullName,

        String phone,

        String identityNumber,

        String insuranceNumber,

        LocalDate dateOfBirth,

        Gender gender,

        Boolean active

) {
}
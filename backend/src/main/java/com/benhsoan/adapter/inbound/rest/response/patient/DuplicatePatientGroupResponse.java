package com.benhsoan.adapter.inbound.rest.response.patient;

import java.time.LocalDate;
import java.util.List;

public record DuplicatePatientGroupResponse(

        String fullName,

        LocalDate dateOfBirth,

        String phone,

        List<PatientResponse> candidates

) {}

package com.benhsoan.port.inbound.patient;

import java.util.List;

import com.benhsoan.port.dto.result.patient.DuplicatePatientGroupResult;

public interface FindDuplicatePatientsUseCase {

    List<DuplicatePatientGroupResult> findDuplicates();
}

package com.benhsoan.port.inbound.patient;

import org.springframework.data.domain.Page;

import com.benhsoan.port.dto.command.patient.SearchPatientCommand;
import com.benhsoan.port.dto.result.PatientResult;

public interface SearchPatientUseCase {

    Page<PatientResult> search(SearchPatientCommand command);

}
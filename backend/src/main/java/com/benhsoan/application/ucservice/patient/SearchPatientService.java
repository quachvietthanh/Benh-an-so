package com.benhsoan.application.ucservice.patient;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.port.dto.command.patient.SearchPatientCommand;
import com.benhsoan.port.dto.result.PatientResult;
import com.benhsoan.port.inbound.patient.SearchPatientUseCase;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchPatientService
        implements SearchPatientUseCase {

    private final PatientRepository patientRepository;
    private final PatientResultMapper patientResultMapper;

    @Override
    public Page<PatientResult> search( SearchPatientCommand command) {
        return patientRepository
            .search(command)
            .map(patientResultMapper::toResult);
        }
}
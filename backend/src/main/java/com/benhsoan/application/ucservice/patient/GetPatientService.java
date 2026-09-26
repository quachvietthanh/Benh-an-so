package com.benhsoan.application.ucservice.patient;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.port.dto.result.PatientResult;
import com.benhsoan.port.inbound.patient.GetPatientByCodeUseCase;
import com.benhsoan.port.inbound.patient.GetPatientByIdUseCase;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPatientService implements GetPatientByIdUseCase, GetPatientByCodeUseCase {

    private final PatientRepository patientRepository;
    private final PatientResultMapper patientResultMapper;

    @Override
    public PatientResult getById(UUID patientId) {
        return patientRepository.findById(patientId)
                .map(this::mapWithMergedCode)
                .orElseThrow(() -> new PatientNotFoundException(patientId));
    }

    @Override
    public PatientResult getByCode(String patientCode) {
        return patientRepository.findByPatientCode(patientCode)
                .map(this::mapWithMergedCode)
                .orElseThrow(() -> new PatientNotFoundException(patientCode));
    }

    private PatientResult mapWithMergedCode(com.benhsoan.domain.patient.Patient patient) {
        if (patient.isMerged() && patient.getMergedIntoPatientId() != null) {
            String mergedIntoPatientCode = patientRepository.findById(patient.getMergedIntoPatientId())
                    .map(com.benhsoan.domain.patient.Patient::getPatientCode)
                    .orElse(null);
            return patientResultMapper.toResult(patient, mergedIntoPatientCode);
        }
        return patientResultMapper.toResult(patient);
    }
}

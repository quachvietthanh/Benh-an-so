package com.benhsoan.application.ucservice.patient;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.patient.PatientChronicDiseaseResult;
import com.benhsoan.port.inbound.patient.GetPatientChronicDiseasesUseCase;
import com.benhsoan.port.outbound.repository.patient.PatientChronicDiseaseRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPatientChronicDiseasesService implements GetPatientChronicDiseasesUseCase {

    private final PatientRepository patientRepository;
    private final PatientChronicDiseaseRepository patientChronicDiseaseRepository;
    private final PatientChronicDiseaseResultMapper resultMapper;

    @Override
    public List<PatientChronicDiseaseResult> getChronicDiseases(UUID patientId) {
        if (patientId == null) {
            throw new ValidationException("Patient ID is required.");
        }
        if (patientRepository.findById(patientId).isEmpty()) {
            throw new PatientNotFoundException(patientId);
        }
        return patientChronicDiseaseRepository.findByPatientIdAndActiveTrue(patientId)
                .stream()
                .map(resultMapper::toResult)
                .toList();
    }
}

package com.benhsoan.application.ucservice.patient;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.patient.PatientAllergyResult;
import com.benhsoan.port.inbound.patient.GetPatientAllergiesUseCase;
import com.benhsoan.port.outbound.repository.patient.PatientAllergyRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPatientAllergiesService implements GetPatientAllergiesUseCase {

    private final PatientRepository patientRepository;
    private final PatientAllergyRepository patientAllergyRepository;
    private final PatientAllResultMapper resultMapper;

    @Override
    public List<PatientAllergyResult> getAllergies(UUID patientId) {
        if (patientId == null) {
            throw new ValidationException("Patient ID is required.");
        }

        if (patientRepository.findById(patientId).isEmpty()) {
            throw new PatientNotFoundException(patientId);
        }

        return patientAllergyRepository.findByPatientIdAndActiveTrue(patientId)
                .stream()
                .map(resultMapper::toResult)
                .toList();
    }
}

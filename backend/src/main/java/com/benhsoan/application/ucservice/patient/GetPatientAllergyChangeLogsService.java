package com.benhsoan.application.ucservice.patient;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.patient.PatientAllergy;
import com.benhsoan.domain.patient.exception.PatientAllergyNotFoundException;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.patient.PatientAllergyChangeLogResult;
import com.benhsoan.port.inbound.patient.GetPatientAllergyChangeLogsUseCase;
import com.benhsoan.port.outbound.repository.patient.PatientAllergyChangeLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientAllergyRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPatientAllergyChangeLogsService implements GetPatientAllergyChangeLogsUseCase {

    private final PatientRepository patientRepository;
    private final PatientAllergyRepository patientAllergyRepository;
    private final PatientAllergyChangeLogRepository changeLogRepository;
    private final PatientAllResultMapper resultMapper;

    @Override
    public List<PatientAllergyChangeLogResult> getChangeLogs(UUID patientId, UUID allergyId) {
        if (patientId == null) {
            throw new ValidationException("Patient ID is required.");
        }
        if (allergyId == null) {
            throw new ValidationException("Allergy ID is required.");
        }

        if (patientRepository.findById(patientId).isEmpty()) {
            throw new PatientNotFoundException(patientId);
        }

        PatientAllergy allergy = patientAllergyRepository.findById(allergyId)
                .orElseThrow(() -> new PatientAllergyNotFoundException(allergyId));

        if (!allergy.getPatientId().equals(patientId)) {
            throw new PatientAllergyNotFoundException(allergyId);
        }

        return changeLogRepository.findByAllergyIdOrderByChangedAtDesc(allergyId)
                .stream()
                .map(resultMapper::toResult)
                .toList();
    }
}

package com.benhsoan.application.ucservice.patient;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.patient.PatientFamilyHistoryResult;
import com.benhsoan.port.inbound.patient.GetPatientFamilyHistoryUseCase;
import com.benhsoan.port.outbound.repository.patient.PatientFamilyHistoryRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPatientFamilyHistoryService implements GetPatientFamilyHistoryUseCase {

    private final PatientRepository patientRepository;
    private final PatientFamilyHistoryRepository patientFamilyHistoryRepository;
    private final PatientFamilyHistoryResultMapper resultMapper;

    @Override
    public List<PatientFamilyHistoryResult> getFamilyHistory(UUID patientId) {
        if (patientId == null) {
            throw new ValidationException("Patient ID is required.");
        }
        if (patientRepository.findById(patientId).isEmpty()) {
            throw new PatientNotFoundException(patientId);
        }
        return patientFamilyHistoryRepository.findByPatientIdAndActiveTrue(patientId)
                .stream()
                .map(resultMapper::toResult)
                .toList();
    }
}

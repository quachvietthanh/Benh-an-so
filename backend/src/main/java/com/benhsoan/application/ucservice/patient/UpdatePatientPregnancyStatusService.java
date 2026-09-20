package com.benhsoan.application.ucservice.patient;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.enums.PregnancyStatus;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.PatientResult;
import com.benhsoan.port.inbound.patient.UpdatePatientPregnancyStatusUseCase;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdatePatientPregnancyStatusService implements UpdatePatientPregnancyStatusUseCase {

    private final PatientRepository patientRepository;
    private final PatientResultMapper patientResultMapper;

    @Override
    public PatientResult update(UUID patientId, PregnancyStatus pregnancyStatus) {
        if (patientId == null) {
            throw new ValidationException("Patient id is required.");
        }

        Patient patient = patientRepository.findByIdForUpdate(patientId)
                .orElseThrow(() -> new PatientNotFoundException(patientId));

        patient.changePregnancyStatus(pregnancyStatus);
        return patientResultMapper.toResult(patientRepository.save(patient));
    }
}

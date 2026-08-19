package com.benhsoan.application.ucservice.carelog;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.port.dto.result.PostCareLogResult;
import com.benhsoan.port.inbound.carelog.GetPatientCareLogsUseCase;
import com.benhsoan.port.outbound.repository.carelog.PostCareLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPatientCareLogsService implements GetPatientCareLogsUseCase {

    private final PostCareLogRepository postCareLogRepository;
    private final PatientRepository patientRepository;
    private final PostCareLogResultMapper resultMapper;
    private final PostCareLogAuthorizer authorizer;

    @Override
    public List<PostCareLogResult> getForPatient(UUID patientId) {
        authorizer.requireStaffOrAdmin();

        patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException(patientId));

        return postCareLogRepository.findByPatientIdOrderByContactedAtDesc(patientId).stream()
                .map(resultMapper::toResult)
                .toList();
    }
}

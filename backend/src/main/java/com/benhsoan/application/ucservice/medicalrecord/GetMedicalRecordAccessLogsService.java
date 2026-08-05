package com.benhsoan.application.ucservice.medicalrecord;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.port.dto.command.medicalrecord.GetMedicalRecordAccessLogsQuery;
import com.benhsoan.port.dto.result.MedicalRecordAccessLogResult;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordAccessLogsUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordAccessLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMedicalRecordAccessLogsService implements GetMedicalRecordAccessLogsUseCase {

    private final MedicalRecordAccessLogRepository accessLogRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicalRecordAuthorizationService authorizationService;
    private final MedicalRecordResultMapper resultMapper;

    @Override
    public Page<MedicalRecordAccessLogResult> getAccessLogs(GetMedicalRecordAccessLogsQuery query) {
        authorizationService.requireReadAccess();
        var pageable = PageRequest.of(query.page(), query.size());
        if (query.medicalRecordId() != null) {
            medicalRecordRepository.findById(query.medicalRecordId())
                    .orElseThrow(() -> new MedicalRecordNotFoundException(query.medicalRecordId()));
            return accessLogRepository.findByMedicalRecordId(query.medicalRecordId(), query.from(), query.to(), pageable)
                    .map(resultMapper::toResult);
        }
        return accessLogRepository.findByPatientId(query.patientId(), query.from(), query.to(), pageable)
                .map(resultMapper::toResult);
    }
}

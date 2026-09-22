package com.benhsoan.application.ucservice.patient;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.patient.PatientImportLog;
import com.benhsoan.domain.patient.exception.PatientImportLogNotFoundException;
import com.benhsoan.port.dto.result.patient.PatientImportLogResult;
import com.benhsoan.port.dto.result.patient.PatientImportRowErrorResult;
import com.benhsoan.port.inbound.patient.GetPatientImportLogsUseCase;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.patient.PatientImportLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPatientImportLogsService implements GetPatientImportLogsUseCase {

    private final PatientImportLogRepository importLogRepository;
    private final UserRepository userRepository;

    @Override
    public Page<PatientImportLogResult> getLogs(Pageable pageable) {
        Page<PatientImportLog> page = importLogRepository.findAll(pageable);
        List<UUID> userIds = page.getContent().stream()
                .map(PatientImportLog::getImportedBy)
                .distinct()
                .toList();

        Map<UUID, String> userNamesById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getFullName));

        return page.map(log -> toResult(log, userNamesById.get(log.getImportedBy())));
    }

    @Override
    public PatientImportLogResult getLogById(UUID id) {
        PatientImportLog log = importLogRepository.findById(id)
                .orElseThrow(() -> new PatientImportLogNotFoundException(id));

        String userName = userRepository.findById(log.getImportedBy())
                .map(User::getFullName)
                .orElse(null);

        return toResult(log, userName);
    }

    private PatientImportLogResult toResult(PatientImportLog log, String importedByName) {
        List<PatientImportRowErrorResult> errorResults = log.getErrors().stream()
                .map(err -> new PatientImportRowErrorResult(
                        err.getRowNumber(),
                        err.getErrorField(),
                        err.getErrorMessage(),
                        err.getRawData()
                ))
                .toList();

        return new PatientImportLogResult(
                log.getId(),
                log.getFileName(),
                log.getFileSize(),
                log.getTotalRows(),
                log.getSuccessRows(),
                log.getErrorRows(),
                log.getDuplicateRows(),
                log.getStatus(),
                log.getImportedBy(),
                importedByName,
                log.getCreatedAt(),
                errorResults
        );
    }
}

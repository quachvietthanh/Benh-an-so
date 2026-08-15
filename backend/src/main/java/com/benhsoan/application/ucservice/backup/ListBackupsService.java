package com.benhsoan.application.ucservice.backup;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.port.dto.result.BackupResult;
import com.benhsoan.port.inbound.backup.ListBackupsUseCase;
import com.benhsoan.port.outbound.repository.backup.BackupRecordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListBackupsService implements ListBackupsUseCase {

    private final BackupRecordRepository backupRecordRepository;
    private final BackupResultMapper resultMapper;
    private final BackupAuthorizer authorizer;

    @Override
    public List<BackupResult> list() {
        authorizer.requireAdmin();

        return backupRecordRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(resultMapper::toResult)
                .toList();
    }
}

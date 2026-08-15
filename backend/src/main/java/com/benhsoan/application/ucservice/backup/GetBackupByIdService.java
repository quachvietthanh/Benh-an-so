package com.benhsoan.application.ucservice.backup;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.backup.BackupRecord;
import com.benhsoan.domain.backup.exception.BackupNotFoundException;
import com.benhsoan.port.dto.result.BackupResult;
import com.benhsoan.port.inbound.backup.GetBackupByIdUseCase;
import com.benhsoan.port.outbound.repository.backup.BackupRecordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetBackupByIdService implements GetBackupByIdUseCase {

    private final BackupRecordRepository backupRecordRepository;
    private final BackupResultMapper resultMapper;
    private final BackupAuthorizer authorizer;

    @Override
    public BackupResult getById(UUID backupId) {
        authorizer.requireAdmin();

        BackupRecord record = backupRecordRepository.findById(backupId)
                .orElseThrow(() -> new BackupNotFoundException(backupId));

        return resultMapper.toResult(record);
    }
}

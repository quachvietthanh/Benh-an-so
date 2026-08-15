package com.benhsoan.application.ucservice.backup;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.backup.BackupRecord;
import com.benhsoan.domain.backup.enums.BackupType;
import com.benhsoan.domain.backup.exception.BackupNotFoundException;
import com.benhsoan.port.outbound.backup.BackupSnapshot;
import com.benhsoan.port.outbound.repository.backup.BackupRecordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class BackupRecordLifecycleService {

    private final BackupRecordRepository backupRecordRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BackupRecord createInProgress(
            String backupCode,
            BackupType backupType,
            String description,
            UUID actorId,
            Instant createdAt
    ) {
        return backupRecordRepository.save(
                BackupRecord.create(backupCode, backupType, description, actorId, createdAt));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BackupRecord markSuccess(UUID backupId, BackupSnapshot snapshot) {
        BackupRecord record = findById(backupId);
        record.markSuccess(snapshot.fileName(), snapshot.content().length);
        return backupRecordRepository.save(record);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID backupId) {
        BackupRecord record = findById(backupId);
        record.markFailed();
        backupRecordRepository.save(record);
    }

    private BackupRecord findById(UUID backupId) {
        return backupRecordRepository.findById(backupId)
                .orElseThrow(() -> new BackupNotFoundException(backupId));
    }
}
